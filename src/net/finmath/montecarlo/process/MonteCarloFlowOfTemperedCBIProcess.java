package net.finmath.montecarlo.process;

import org.apache.commons.math3.distribution.*;

import cern.jet.stat.Gamma;

import java.lang.Math;
import java.util.function.DoubleUnaryOperator;

import net.finmath.montecarlo.*;
import net.finmath.stochastic.*;
import net.finmath.randomnumbers.*;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * This class stands for a Monte Carlo simulation of a flow of tempered alpha-stable CBI processes.
 * @author Szulda Guillaume
 */
public class MonteCarloFlowOfTemperedCBIProcess implements MonteCarloCBIProcessInterface {
	
	private FlowOfTemperedAlphaStableCBIprocess cbiProcess;
	private int numberOfPaths;
	private int seed;
	private TimeDiscretizationInterface timeDiscretization;
	private RandomVariableInterface[][] increments;
	
	/**
	 * First constructor, creates an instance of the MonteCarloFlowOfTemperedCBIProcess class, representing a Monte Carlo simulation
	 * of some flow denoted by the input paramter cbiProcess.
	 * @param seed
	 * @param numberOfPaths
	 * @param timeDiscretization
	 * @param cbiProcess
	 * @throws IllegalArgumentException
	 */
	public MonteCarloFlowOfTemperedCBIProcess(int seed, int numberOfPaths, TimeDiscretizationInterface timeDiscretization, FlowOfTemperedAlphaStableCBIprocess cbiProcess) throws IllegalArgumentException {
		if(timeDiscretization.getTime(timeDiscretization.getNumberOfTimeSteps()) != cbiProcess.getTimeHorizon() || timeDiscretization.getNumberOfTimeSteps() != cbiProcess.getNumberOfTimeSteps()) {
			throw new IllegalArgumentException("The Monte Carlo time discretization must be coincide with the validation domain of the CBI process.");
		} else {
			this.numberOfPaths = numberOfPaths;
			this.seed = seed;
			this.timeDiscretization = timeDiscretization;
			this.cbiProcess = cbiProcess;
			// Memory allocation for the simulations of the increments of the processes :
			this.increments = new RandomVariableInterface[cbiProcess.getDimension()][timeDiscretization.getNumberOfTimeSteps()];
			// Computation of the variates standing for the increments of the processes, embedding all the paths required for the simulation :
			generateIncrements();
		}
	}
	
	/**
	 * Second and ultimate constructor, creates an instance of the MonteCarloFlowOfTemperedCBIProcess class, 
	 * representing a Monte Carlo simulation of some flow that has yet to be created by means of the following input parameters:
	 * @param initialValues
	 * @param immigrationRates
	 * @param b
	 * @param sigma
	 * @param eta
	 * @param zeta
	 * @param alpha
	 * @param lambda
	 * @param timeHorizon
	 * The following parameters represent the features of the simulation itself :
	 * @param seed
	 * @param numberOfPaths
	 * @param timeDiscretization
	 * @throws IllegalArgumentException
	 */
	public MonteCarloFlowOfTemperedCBIProcess(double timeHorizon, int numberOfTimeSteps, int seed, int numberOfPaths, TimeDiscretizationInterface timeDiscretization, double[] initialValues, double[] immigrationRates, double b, double sigma, double eta, double zeta, double alpha, double[] lambda) throws IllegalArgumentException {
		if(timeDiscretization.getTime(timeDiscretization.getNumberOfTimeSteps()) != timeHorizon || timeDiscretization.getNumberOfTimeSteps() != numberOfTimeSteps) {
			throw new IllegalArgumentException("The Monte Carlo time discretization must be coincide with the validation domain of the CBI process.");
		} else {
			this.numberOfPaths = numberOfPaths;
			this.seed = seed;
			this.timeDiscretization = timeDiscretization;
			this.cbiProcess = new FlowOfTemperedAlphaStableCBIprocess(timeHorizon, numberOfTimeSteps, initialValues, immigrationRates, b, sigma, eta, zeta, alpha, lambda);
			this.increments = new RandomVariableInterface[initialValues.length][timeDiscretization.getNumberOfTimeSteps()];
			generateIncrements();
		}
	}
	
	@Override
	public double getTimeHorizon() {
		return this.getCBIProcess().getTimeHorizon();
	}
	
	@Override 
	public int getNumberOfTimeSteps() {
		return this.getCBIProcess().getNumberOfTimeSteps();
	}
	
	@Override
	public long getSeed() {
		Integer y = this.seed;
		return y.longValue();
	}
	
	@Override
	public int getNumberOfComponents() {
		return cbiProcess.getDimension();
	}
	
	@Override
	public int getNumberOfPaths() {
		return this.numberOfPaths;
	}
	
	@Override
	public TimeDiscretizationInterface getTimeDiscretization() {
		return this.timeDiscretization;
	}
	
	@Override
	public double getTime(int timeIndex) {
		return (this.timeDiscretization).getTime(timeIndex);
	}
	
	@Override
	public int getTimeIndex(double time) {
		if((this.timeDiscretization).getTimeIndex(time) < 0) {
			return (this.timeDiscretization).getTimeIndexNearestLessOrEqual(time);
		} else {
			return (this.timeDiscretization).getTimeIndex(time);
		}
	}
	
	@Override
	public MonteCarloCBIProcessInterface getCloneWithModifiedTimeDiscretization(TimeDiscretizationInterface newTimeDiscretization) {
		return new MonteCarloFlowOfTemperedCBIProcess(seed, numberOfPaths, newTimeDiscretization, cbiProcess);
	}
	
	@Override
	public RandomVariableInterface getRandomVariableForConstant(double value) {
		return new RandomVariable(0.0, numberOfPaths, value);
	}
	
	@Override
	public MonteCarloCBIProcessInterface getCloneWithModifiedSeed(int newSeed) {
		return new MonteCarloFlowOfTemperedCBIProcess(newSeed, numberOfPaths, timeDiscretization, cbiProcess);
	}
	
	@Override
	public FlowOfTemperedAlphaStableCBIprocess getCBIProcess() {
		return this.cbiProcess;
	}

	@Override
	public RandomVariableInterface getIncrement(int timeIndex, int factor) {
		return increments[factor][timeIndex];
	}
	
	@Override
	public RandomVariableInterface getCBIProcessValue(int timeIndex, int factorIndex) {
		RandomVariableInterface x = new RandomVariable(0.0, this.numberOfPaths, (this.cbiProcess).getInitialValues()[factorIndex]);
		for(int i = 0; i < timeIndex; i++) {
			x = x.add(increments[factorIndex][i]);
		}
		return x;
	}
	
	@Override
	public RandomVariableInterface getCBIProcessValue(double time, int factorIndex) {
		return this.getCBIProcessValue(this.getTimeIndex(time), factorIndex);
	}
	
	@Override
	public RandomVariableInterface getProcessValue(int timeIndex, int component) {
		return getCBIProcessValue(timeIndex, component);
	}

	@Override
	public RandomVariableInterface getMonteCarloWeights(int timeIndex) {
		return new RandomVariable(this.getTime(timeIndex), this.numberOfPaths, 1.0 /(double)(this.numberOfPaths));
	}
	
	@Override
	public RandomVariableInterface getMonteCarloWeights(double time) {
		return new RandomVariable(time, this.numberOfPaths, 1.0 /(double)(this.numberOfPaths));
	}

	@Override
	public ProcessInterface clone() {
		return new MonteCarloFlowOfTemperedCBIProcess(seed, numberOfPaths, timeDiscretization, cbiProcess);
	}
	
	/**
	 * This method is used to generate the random variables representing the increments of the processes to simulate. 
	 */
	private void generateIncrements() {
		double epsilon = 0.001d;
		MultiDimensionalMersenneTwister rng = new MultiDimensionalMersenneTwister(seed, 2);
		double acceptanceLevel = (Math.pow(cbiProcess.getZeta()*epsilon, -cbiProcess.getAlpha())*Math.exp(-cbiProcess.getZeta()*epsilon))/(cbiProcess.getAlpha()*Gamma.incompleteGammaComplement(-cbiProcess.getAlpha(), cbiProcess.getZeta()*epsilon));
		RandomNumberGenerator arm = new AcceptanceRejectionRandomNumberGenerator(rng, new DoubleUnaryOperator() {
			@Override
			public double applyAsDouble(double v) {
				return (Math.exp(-cbiProcess.getZeta()*v))/(Gamma.incompleteGammaComplement(-cbiProcess.getAlpha(), cbiProcess.getZeta()*epsilon)*v*Math.pow(v*cbiProcess.getZeta(), cbiProcess.getAlpha()));
			}
		}, new DoubleUnaryOperator() {
			@Override
			public double applyAsDouble(double v) {
				return (cbiProcess.getAlpha()*Math.pow(epsilon, cbiProcess.getAlpha()))/(Math.pow(v, 1+cbiProcess.getAlpha()));
			}
		}, new DoubleUnaryOperator() {
			@Override
			public double applyAsDouble(double v) {
				return epsilon*Math.pow(v, -1.0/cbiProcess.getAlpha());
			}
		}, acceptanceLevel);
		
		NormalDistribution g = new NormalDistribution(rng.getOneDimMersenneTwister(), 0, 1);
		
		double[][][] incrementsValues = new double[getNumberOfComponents()][timeDiscretization.getNumberOfTimeSteps()][numberOfPaths];
		
		for(int path = 0; path < numberOfPaths; path++) {
			
			for(int factor = 0; factor < getNumberOfComponents(); factor++) {
				
				double x = cbiProcess.getInitialValues()[factor];
				double dx = 0;
				
				for(int timeIndex = 0; timeIndex < timeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
					
					double dt = timeDiscretization.getTimeStep(timeIndex);
					PoissonDistribution p = new PoissonDistribution(rng.getOneDimMersenneTwister(), (-dt*x*Math.pow(cbiProcess.getZeta(), cbiProcess.getAlpha())*Gamma.incompleteGammaComplement(-cbiProcess.getAlpha(), cbiProcess.getZeta()*epsilon))/(Math.cos(Math.PI*0.5*cbiProcess.getAlpha())*Gamma.gamma(-cbiProcess.getAlpha())), PoissonDistribution.DEFAULT_EPSILON, PoissonDistribution.DEFAULT_MAX_ITERATIONS);
				    double sum = 0;
					for(int i = 0; i < p.sample(); i++) {
				    	sum = sum + arm.getNext()[0];
				    }
					dx = sum + cbiProcess.getSigma()*Math.sqrt(Math.abs(x)*dt)*g.sample() + ((cbiProcess.getImmigrationRates()[factor]-cbiProcess.getB()*x)-((cbiProcess.getEta()*cbiProcess.getAlpha()*x*Math.pow(cbiProcess.getZeta(), cbiProcess.getAlpha()-1)*Gamma.incompleteGammaComplement(1-cbiProcess.getAlpha(), cbiProcess.getZeta()*epsilon)) / (Gamma.gamma(1-cbiProcess.getAlpha())*Math.cos(Math.PI*cbiProcess.getAlpha()*0.5))))*dt;
					incrementsValues[factor][timeIndex][path] = dx;
					x = x + dx;
					x = Math.abs(x);
					
				}
				
			}
			
		}
		
		for(int factor = 0; factor < getNumberOfComponents(); factor++) {
			for(int timeIndex = 0; timeIndex < timeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
				increments[factor][timeIndex] = new RandomVariable(timeDiscretization.getTime(timeIndex+1), incrementsValues[factor][timeIndex]);
			}
		}
			
	}
	
}
