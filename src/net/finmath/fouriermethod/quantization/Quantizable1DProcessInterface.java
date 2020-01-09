package net.finmath.fouriermethod.quantization;

import net.finmath.fouriermethod.calibration.models.CalibrableProcessInterface;
import net.finmath.modelling.ModelInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Interface representing the Fourier-based quadratic quantization (order 2), of a scalar random variable standing for the evaluation at "maturity",
 * of a one-dimensional underlying process (CalibrableProcessInterface model) whose characteristic function is known in closed-form (required for setting up).
 * This one then contains the optimal (or sub-optimal, stationary) quantization grid (quantizer) for some level of the state variable.
 * It also contains the corresponding Voronoi quantization of the variate,
 * along with its companions weights describing its law over the associated grid.
 * Likewise, it provides the resulting quantization error between the variate and its quantization.
 * 
 * @author Szulda Guillaume
 */
public interface Quantizable1DProcessInterface extends ModelInterface {
	
	double getMaturity();
	
	int getLevel();
	
	CalibrableProcessInterface getUnderlyingModel();
	
	double getQuantizationError();
	
	double[] getQuantizationGrid();
	
	RandomVariableInterface getVoronoiQuantization();
	
	RandomVariableInterface getCompanionWeights();
	
	/**
	 * Calibration substitutes in the model the parameters of the process with calibrated ones.
	 * Market observables such as the initial stock value should not be changed.
	 * @param parameters
	 * @return a clone of the original model with modified parameters.
	 * Note that this deals with the underlying process (model) of the quantization procedure,
	 * only its parameters need to be modified via calibration, not the parameters themselves of the quantization.
	 */
	Quantizable1DProcessInterface getCloneForModifiedParameters(double[] parameters);
	
}