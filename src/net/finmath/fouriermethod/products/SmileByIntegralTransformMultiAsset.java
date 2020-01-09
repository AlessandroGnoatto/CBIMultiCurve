package net.finmath.fouriermethod.products;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.fouriermethod.CharacteristicFunctionInterface;
import net.finmath.fouriermethod.models.MultivariateProcessCharacteristicFunctionInterface;

public interface SmileByIntegralTransformMultiAsset extends CharacteristicFunctionInterface{
	
	/**
	 * Return the maturity of the associated payoff.
	 * 
	 * @return The maturity of the associated payoff.
	 */
	public double getMaturity();
	
	/**
	 * Return the lower bound of the imaginary part of the domain where
	 * the characteristic function can be integrated.
	 * 
	 * @return the lower bound of the imaginary part of the domain of integration.
	 */
	public double getIntegrationDomainImagLowerBound();
	
	/**
	 * Return the upper bound of the imaginary part of the domain where
	 * the characteristic function can be integrated.
	 * 
	 * @return the upper bound of the imaginary part of the domain of integration.
	 */
	public double getIntegrationDomainImagUpperBound();
	
	
	/**
	 * Return the price of a family of options with the same maturity for different strikes
	 * @param model
	 * @return
	 * @throws CalculationException
	 */
	public Map<Double, Double> getValue(MultivariateProcessCharacteristicFunctionInterface model) throws CalculationException;

}
