package net.finmath.fouriermethod.calibration.models;

import net.finmath.fouriermethod.models.MultivariateProcessCharacteristicFunctionInterface;

public interface MultivariateCalibrableProcessInterface extends MultivariateProcessCharacteristicFunctionInterface{
	
	/**
	 * This method is needed for the corresponding model to be passed to the FFT pricing routine.
	 * @return
	 */
	MultivariateProcessCharacteristicFunctionInterface getCharacteristiFunction();
	
	
	/**
	 * Calibration substitutes in the model the parameters of the process with calibrated ones.
	 * Market observables such as the initial stock value should not be changed.
	 * @param parameters
	 * @return a clone of the original model with modified parameters.
	 */
	MultivariateCalibrableProcessInterface getCloneForModifiedParameters(double[] parameters);

	/*
	 * Upper and lower bounds have to be collected for them to be passed to the factory of the optimization algorithm.
	 * In this way we guarantee consistency between the constraints in the model
	 * and the constraints in the optimizer factory.
	 */
	double[] getParameterUpperBounds();
	
	double[] getParameterLowerBounds();
	
	double[] getParameters();

}
