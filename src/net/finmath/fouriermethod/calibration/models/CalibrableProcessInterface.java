package net.finmath.fouriermethod.calibration.models;

import net.finmath.fouriermethod.models.ProcessCharacteristicFunctionInterface;

/**
 * Every class implementing this interface communicates with the calibration routine by providing
 * clones of the model with changed parameters. 
 * 
 * We are decorating every characteristic function with the getCloneForModifiedParameters without touching
 * the existing classes providing the computation of the characteristic function.
 * 
 * Suitable specifications of getCloneForModifiedParameters can be employed to introduce e.g. non-linear constraints.
 * E.g. it is possible to force the Feller condition in the Heston model by providing a suitable implementation of
 * this method.
 * 
 * @author Alessandro Gnoatto
 *
 */
public interface CalibrableProcessInterface extends ProcessCharacteristicFunctionInterface{
	
	/**
	 * This method is needed for the corresponding model to be passed to the FFT pricing routine.
	 * @return
	 */
	ProcessCharacteristicFunctionInterface getCharacteristiFunction();
	
	/**
	 * Calibration substitutes in the model the parameters of the process with calibrated ones.
	 * Market observables such as the initial stock value should not be changed.
	 * @param parameters
	 * @return a clone of the original model with modified parameters.
	 */
	CalibrableProcessInterface getCloneForModifiedParameters(double[] parameters);
	
	/*
	 * Upper and lower bounds have to be collected for them to be passed to the factory of the optimization algorithm.
	 * In this way we guarantee consistency between the constraints in the model
	 * and the constraints in the optimizer factory.
	 */
	double[] getParameterUpperBounds();
	
	double[] getParameterLowerBounds();

}
