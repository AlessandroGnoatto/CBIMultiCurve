package net.finmath.fouriermethod.calibration;

import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

import net.finmath.exception.CalculationException;
import net.finmath.fouriermethod.calibration.models.MultivariateCalibrableProcessInterface;
import net.finmath.fouriermethod.products.EuropeanOptionSmileMultiAsset;
import net.finmath.marketdata.model.volatilities.*;
import net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface.QuotingConvention;
import net.finmath.optimizer.*;

public class CapletCalibrationProblem {
	
	private final CapletSurfaceData surface; //target calibration instruments. They dictate the calibration entity: vol/price.
	private final MultivariateCalibrableProcessInterface model; //Pricing model
	private final OptimizerFactoryInterface optimizerFactory; //construct the instance of the optimization algorithm inside the class.
	private final EuropeanOptionSmileMultiAsset pricer; //How do we compute prices: Carr Madan, Cos, Conv, Lewis...
	
	//Optimizer parameters
	private final double[] initialParameters;
	private final double[] lowerBound;
	private final double[] upperBound;
	private final double[] parameterStep;
	
	public CapletCalibrationProblem(CapletSurfaceData surface, MultivariateCalibrableProcessInterface model,
			OptimizerFactoryInterface optimizerFactory, EuropeanOptionSmileMultiAsset pricer, double[] initialParameters,
			double[] parameterStep) {
		super();
		this.surface = surface;
		this.model = model;
		this.optimizerFactory = optimizerFactory;
		this.pricer = pricer;
		this.initialParameters = initialParameters;
		this.lowerBound = model.getParameterLowerBounds();
		this.upperBound = model.getParameterUpperBounds();
		this.parameterStep = parameterStep;
	}
	
	public OptimizationResult runCalibration() throws SolverException {
		
		OptimizerInterface.ObjectiveFunction objectiveFunction = new OptimizerInterface.ObjectiveFunction() {

			@Override
			public void setValues(double[] parameters, double[] values) throws SolverException {
				long tstart = System.currentTimeMillis();
				MultivariateCalibrableProcessInterface newModel = model.getCloneForModifiedParameters(parameters);
				
				int numberOfMaturities = surface.getMaturities().length;
				double mats[] = surface.getMaturities();
				
				QuotingConvention targetConvention = surface.getConvention(); 
				
				ArrayList<Double> vals = new ArrayList<Double>();
								
				for(int t = 0; t<numberOfMaturities; t++) {
					double[] currentStrikes = surface.getSmile(mats[t]).getStrikes();
					String underlying = surface.getSmile(mats[t]).getUnderlyingCurve();
					String discountCurveName = surface.getSmile(mats[t]).getDiscountCurve();
					double delta = underlyingToTenor(underlying);
					
					EuropeanOptionSmileMultiAsset newPricer = pricer.getCloneWithModifiedParameters(underlying,mats[t],currentStrikes);
					
					try {
						Map<Double, Double> currentModelPrices = newPricer.getValue(newModel);
						
						for(int i = 0; i<currentStrikes.length;i++) {
							
							if(targetConvention.equals(QuotingConvention.VOLATILITYLOGNORMAL)) {
								//we convert prices into lognormal volatilities
								double forward = surface.getCurves().getForwardCurve(underlying).getValue(mats[t]-delta);
								double optionMaturity = mats[t];
								double optionStrike = currentStrikes[i];
								double payoffUnit = delta * surface.getCurves().getDiscountCurve(discountCurveName).getDiscountFactor(mats[t]);
								double optionValue = currentModelPrices.get(currentStrikes[i]);
																
								vals.add(net.finmath.functions.AnalyticFormulas.blackScholesOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, optionValue));
								
							
							}else if(targetConvention.equals(QuotingConvention.VOLATILITYNORMAL)) {
								//we convert prices into normal volatilities
								double forward = surface.getCurves().getForwardCurve(underlying).getValue(mats[t]-delta);
								double optionMaturity = mats[t];
								double optionStrike = currentStrikes[i];
								double payoffUnit = delta * surface.getCurves().getDiscountCurve(discountCurveName).getDiscountFactor(mats[t]);
								double optionValue = currentModelPrices.get(currentStrikes[i]);
								vals.add(net.finmath.functions.AnalyticFormulas.bachelierOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, optionValue));
							
							}else {
								//just output the prices
								vals.add(currentModelPrices.get(currentStrikes[i]));
							}						
							
						}
						
					} catch (CalculationException e) {
						e.printStackTrace();
					}
				}
				for(int i = 0; i< values.length; i++)
					values[i] = vals.get(i);

				long tend = System.currentTimeMillis();
				System.out.println("Iteration required: " + (tend-tstart)/1000.0 + "seconds.");
			}
			
		};
		
		OptimizerInterface optimizer = optimizerFactory.getOptimizer(
				objectiveFunction,
				initialParameters,
				lowerBound,
				upperBound,
				parameterStep,
				formatTargetValuesForOptimizer() /* targetValues */);
		
		
		optimizer.run();
		
		ArrayList<String> calibrationOutput = outputCalibrationResult(optimizer.getBestFitParameters()); 
	         
		MultivariateCalibrableProcessInterface calibratedModel = model.getCloneForModifiedParameters(optimizer.getBestFitParameters());
	         
	    return new OptimizationResult(calibratedModel,optimizer.getBestFitParameters(),optimizer.getIterations(),optimizer.getRootMeanSquaredError(),calibrationOutput);
		
	}
	
	/**
	 * This is a service method that takes care of putting all the target values in a single array.
	 * @return
	 */
	private double[] formatTargetValuesForOptimizer() {
		//Put all values in an array for the optimizer.
		int numberOfMaturities = surface.getMaturities().length;
		double mats[] = surface.getMaturities();
		
		ArrayList<Double> vals = new ArrayList<Double>();
		
		for(int t = 0; t<numberOfMaturities; t++) {
			double mat = mats[t];
			double[] myStrikes = surface.getSurface().get(mat).getStrikes();
			
			CapletSmileData smileOfInterest = surface.getSurface().get(mat);
			
			for(int k = 0; k < myStrikes.length; k++) {
				vals.add(smileOfInterest.getSmile().get(myStrikes[k]).getValue());
			}
					
		}
		Double[] targetVals = new Double[vals.size()];
		return ArrayUtils.toPrimitive(vals.toArray(targetVals));
	}
	
	/**
	 * When the calibration is over this method is called to produce a table
	 * @param parameters
	 */
	private ArrayList<String> outputCalibrationResult(double[] parameters) {
		
		ArrayList<String> calibrationOutput = new ArrayList<String>();
		
		MultivariateCalibrableProcessInterface newModel = model.getCloneForModifiedParameters(parameters);
		
		int numberOfMaturities = surface.getMaturities().length;
		double mats[] = surface.getMaturities();
		
		QuotingConvention targetConvention = surface.getConvention();
		
		double value;
		double targetValue;
		double T;
		double K;
		
		calibrationOutput.add("Strike"+ "\t" + "Maturity"+ "\t" + "Market Value" + "\t" + "Model Value" + "\t" + "Squared Error");
		
		for(int t = 0; t<numberOfMaturities; t++) {
			T = mats[t];
			
			CapletSmileData currentSmile = surface.getSmile(mats[t]);
			double[] currentStrikes = currentSmile.getStrikes();
			
			String underlying = surface.getSmile(mats[t]).getUnderlyingCurve();
			String discountCurveName = surface.getSmile(mats[t]).getDiscountCurve();
			double delta = underlyingToTenor(underlying);
			
			EuropeanOptionSmileMultiAsset newPricer = pricer.getCloneWithModifiedParameters(underlying,mats[t],currentStrikes);
			
			try {
				Map<Double, Double> currentModelPrices = newPricer.getValue(newModel);
				
				for(int i = 0; i<currentStrikes.length;i++) {
					K = currentStrikes[i];
					targetValue = currentSmile.getOption(currentStrikes[i]).getValue();
					
					if(targetConvention.equals(QuotingConvention.VOLATILITYLOGNORMAL)) {
						//we convert prices into lognormal volatilities
						double forward = surface.getCurves().getForwardCurve(underlying).getValue(mats[t]-delta);
						double optionMaturity = mats[t];
						double optionStrike = currentStrikes[i];
						double payoffUnit = delta * surface.getCurves().getDiscountCurve(discountCurveName).getDiscountFactor(mats[t]);
						double optionValue = currentModelPrices.get(currentStrikes[i]);
														
						value = net.finmath.functions.AnalyticFormulas.blackScholesOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, optionValue);
						
					
					}else if(targetConvention.equals(QuotingConvention.VOLATILITYNORMAL)) {
						//we convert prices into normal volatilities
						double forward = surface.getCurves().getForwardCurve(underlying).getValue(mats[t]-delta);
						double optionMaturity = mats[t];
						double optionStrike = currentStrikes[i];
						double payoffUnit = delta * surface.getCurves().getDiscountCurve(discountCurveName).getDiscountFactor(mats[t]);
						double optionValue = currentModelPrices.get(currentStrikes[i]);
						
						value = net.finmath.functions.AnalyticFormulas.bachelierOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, optionValue);
					}else {
						//just output the prices
						value = currentModelPrices.get(currentStrikes[i]);
					}
					calibrationOutput.add(K+ "\t" + T + "\t" + targetValue + "\t" + value+ "\t" + Math.pow(targetValue-value,2));
				}
			} catch (CalculationException e) {
				e.printStackTrace();
			}

		}		
		return calibrationOutput;
	}
	
	/**
	 * Helper class for calibration results.
	 * @author Alessandro Gnoatto
	 *
	 */
	public class OptimizationResult{
		private final MultivariateCalibrableProcessInterface model; //the calibrated model
		private final double[] bestFitParameters;
		private final int iterations;
		private final double rootMeanSquaredError;
		private final ArrayList<String> calibrationOutput;
		
		public OptimizationResult(MultivariateCalibrableProcessInterface model, double[] bestFitParameters,
				int iterations, double rootMeanSquaredError, ArrayList<String> calibrationOutput) {
			super();
			this.model = model;
			this.bestFitParameters = bestFitParameters;
			this.iterations = iterations;
			this.rootMeanSquaredError = rootMeanSquaredError;
			this.calibrationOutput = calibrationOutput;
		}

		public MultivariateCalibrableProcessInterface getModel() {
			return model;
		}
		
		public double[] getBestFitParameters() {
			return bestFitParameters;
		}

		public int getIterations() {
			return iterations;
		}

		public double getRootMeanSquaredError() {
			return rootMeanSquaredError;
		}
		
		public ArrayList<String> getCalibrationOutput(){
			return this.calibrationOutput;
		}
		
	}
	
	private double underlyingToTenor(String underlyingName) {
		if(underlyingName.contains("3M")) {
			return 0.25;
		}else if(underlyingName.contains("6M")) {
			return 0.5;
		}else if(underlyingName.contains("12M")) {
			return 1.0;
			
		}else if(underlyingName.contains("1Y")) {
			return 1.0;
			
		}else {
			throw new IllegalArgumentException("Tenor not recognized.");
		}
	}


}
