package net.finmath.timeseries;

import java.util.Iterator;
import java.util.function.DoubleUnaryOperator;

import net.finmath.time.TimeDiscretization;

public class FunctionVMinusOne implements TimeSeriesInterface {
	
	private final double[] values;
	private final TimeDiscretization timeGrid;
	private final double lambda;
	private final DoubleUnaryOperator psi;
	
	public FunctionVMinusOne(double timeHorizon, int numberOfTimeSteps, double lambda, DoubleUnaryOperator psi) {
		this.lambda = lambda;
		this.psi = psi;
		double deltaT = timeHorizon / (double)(numberOfTimeSteps);
		this.timeGrid = new TimeDiscretization(0.0, numberOfTimeSteps, deltaT);
		this.values = new double[numberOfTimeSteps+1];
		values[0] = -1;
		for(int i = 1; i < numberOfTimeSteps+1; i++) {
			double k1 = (this.lambda - (this.psi).applyAsDouble(values[i-1]))*deltaT;
			double k2 = (this.lambda - (this.psi).applyAsDouble(values[i-1]+ k1*0.5*deltaT))*deltaT;
			double k3 = (this.lambda - (this.psi).applyAsDouble(values[i-1]+ k2*0.5*deltaT))*deltaT;
			double k4 = (this.lambda - (this.psi).applyAsDouble(values[i-1] + k3*deltaT))*deltaT;
			values[i] = (k1 + k2*2 + k3*2 + k4)*(1.0 / 6.0) + values[i-1];
		}
	}

	@Override
	public double getTime(int index) {
		return this.timeGrid.getTime(index);
	}

	@Override
	public double getValue(int index) {
		return this.values[index];
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
						return index < FunctionVMinusOne.this.getNumberOfTimePoints();
					}

					@Override
					public Double next() {
						return FunctionVMinusOne.this.getValue(index++);
					}
				};
			}

		};
	}
	
	public double getValue(double time) {
		if(this.timeGrid.getTimeIndex(time) < 0) {
			return this.values[this.timeGrid.getTimeIndexNearestLessOrEqual(time)];
		} else {
			return this.values[this.timeGrid.getTimeIndex(time)];
		}
	}
	
	public double getIntegral(double firstTime, double lastTime) {
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
		double sum = 0;
		for(int i = firstIndex; i < lastIndex + 1; i++) {
			sum = sum + this.values[i]*this.timeGrid.getTimeStep(i);
		}
		return sum;
	}

}