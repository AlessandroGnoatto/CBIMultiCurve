package net.finmath.fouriermethod.models;

import net.finmath.fouriermethod.CharacteristicFunctionInterface;
import net.finmath.modelling.ModelInterface;

public interface MultivariateProcessCharacteristicFunctionInterface extends ModelInterface{
	
	/**
	 * Returns the characteristic function of X(t), where X is <code>this</code> stochastic process.
	 * X is multivariate process and each component of X has a name that uniquely identifies it.
	 * 
	 * @param time The time at which the stochastic process is observed.
	 * @return The characteristic function of X(t).
	 */
	CharacteristicFunctionInterface apply(double time, String underlyingName);

}
