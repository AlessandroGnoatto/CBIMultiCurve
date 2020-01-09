package net.finmath.timeseries;

import java.util.Iterator;
import java.util.function.UnaryOperator;

import org.apache.commons.math3.complex.Complex;

import net.finmath.time.TimeDiscretization;

public class FunctionW implements TimeSeriesInterface {
	
	private final Complex[] values;
	private final TimeDiscretization timeGrid;
	private final double lambda;
	private final UnaryOperator<Complex> cpsi;
	
	public FunctionW(double timeHorizon, int numberOfTimeSteps, double lambda, UnaryOperator<Complex> cpsi, Complex u) {
		this.lambda = lambda;
		this.cpsi = cpsi;
		double deltaT = timeHorizon / (double)(numberOfTimeSteps);
		this.timeGrid = new TimeDiscretization(0.0, numberOfTimeSteps, deltaT);
		this.values = new Complex[numberOfTimeSteps+1];
		values[0] = u.negate();
		for(int i = 1; i < numberOfTimeSteps+1; i++) {
			Complex k1 = (new Complex(this.lambda, 0).subtract((this.cpsi).apply(values[i-1]))).multiply(deltaT);
			Complex k2 = (new Complex(this.lambda, 0).subtract((this.cpsi).apply(values[i-1].add(k1.multiply(deltaT*0.5))))).multiply(deltaT);
			Complex k3 = (new Complex(this.lambda, 0).subtract((this.cpsi).apply(values[i-1].add(k2.multiply(deltaT*0.5))))).multiply(deltaT);
			Complex k4 = (new Complex(this.lambda, 0).subtract((this.cpsi).apply(values[i-1].add(k3.multiply(deltaT))))).multiply(deltaT);
			values[i] = ((k1.add(k2.multiply(2)).add(k3.multiply(2)).add(k4)).divide(6.0)).add(values[i-1]);
		}
	}

	@Override
	public double getTime(int index) {
		return this.timeGrid.getTime(index);
	}

	@Override
	public double getValue(int index) {
		return this.values[index].getReal();
	}

	@Override
	public int getNumberOfTimePoints() {
		return this.values.length;
	}

	@Override
	public Iterable<Double> getValues() {
		return new Iterable<Double>() {
			private int index = 0;

			@Override
			public Iterator<Double> iterator() {
				return new Iterator<Double>() {
					@Override
					public boolean hasNext() {
						return index < FunctionW.this.getNumberOfTimePoints();
					}

					@Override
					public Double next() {
						return FunctionW.this.getValue(index++);
					}
				};
			}

		};
	}
	
	public Complex getValue(double time) {
		if(this.timeGrid.getTimeIndex(time) < 0) {
			return this.values[this.timeGrid.getTimeIndexNearestLessOrEqual(time)];
		} else {
			return this.values[this.timeGrid.getTimeIndex(time)];
		}
	}
	
	public Complex getIntegral(double firstTime, double lastTime) {
		int firstIndex = 0;
		int lastIndex = 0;
		if(this.timeGrid.getTimeIndex(firstTime) < 0) {
			firstIndex = this.timeGrid.getTimeIndexNearestLessOrEqual(firstTime);
		} else {
			firstIndex = this.timeGrid.getTimeIndex(firstTime);
		}
		if(this.timeGrid.getTimeIndex(lastTime) < 0) {
			lastIndex = this.timeGrid.getTimeIndexNearestLessOrEqual(lastTime);
		} else {
			lastIndex = this.timeGrid.getTimeIndex(lastTime);
		}
		Complex sum = new Complex(0,0);
		for(int i = firstIndex; i < lastIndex + 1; i++) {
			sum = sum.add(this.values[i].multiply(this.timeGrid.getTimeStep(i)));
		}
		return sum;
	}

}