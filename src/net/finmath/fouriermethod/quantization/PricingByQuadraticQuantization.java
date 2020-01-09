package net.finmath.fouriermethod.quantization;

import net.finmath.exception.CalculationException;
import net.finmath.modelling.*;

/**
 * Abstract class dealing with derivative pricing via Quadratic Quantization (Quantization pricer at some level N of roder 2).
 * This is made possible by means of the cubature formula for the expectation of the payoff,
 * available if the payoff function of the underlying quantizable process is smooth enough.
 * This formula allows to approximate the time-0 price by the expectation of the payoff function applied to the Voronoi quantization,
 * which is computable thanks to the discrete feature of the quantization over the corresponding quantization grid,
 * for the associated companion weights (describing its distribution).
 * 
 * @author Szulda Guillaume.
 */
public abstract class PricingByQuadraticQuantization implements ProductInterface {
	
	private final String currency;
	
	public PricingByQuadraticQuantization(String currency) {
		this.currency = currency;
	}

	public PricingByQuadraticQuantization() {
		this.currency = null;
	}

	public String getCurrency() {
		return currency;
	}
	
	@Override
	public Object getValue(ModelInterface model) throws IllegalArgumentException {
		if(model instanceof Quantizable1DProcessInterface) {
			try {
				return getValue((Quantizable1DProcessInterface)model);
			} catch (IllegalArgumentException e) {
				return null;
			}
		} else if(model instanceof QuantizableMultiDimProcessInterface) {
			try {
				return getValue((QuantizableMultiDimProcessInterface)model);
			} catch (IllegalArgumentException e) {
				return null;
			}
		} else {
			throw new IllegalArgumentException("The product " + this.getClass()
			+ " cannot be valued against a model " + model.getClass() + "."
			+ "It requires a model of type " + Quantizable1DProcessInterface.class + ".");
		}
	}
	
	/**
	 * Method providing the time-0 price of the product to price by quantizing the underlying of its payoff,
	 * quantizing it is modeled by the @param model of "Quantizable1DProcessInterface".
	 * @param model
	 * @return
	 * @throws CalculationException
	 * @throws IllegalArgumentException
	 */
	public abstract Double getValue(Quantizable1DProcessInterface model) throws IllegalArgumentException;
	
	/**
	 * Method providing the time-0 price of the product to price by quantizing the underlying of its payoff,
	 * quantizing it is modeled by the @param model of "QuantizableMultiDimProcessInterface".
	 * @param model
	 * @return
	 * @throws CalculationException
	 * @throws IllegalArgumentException
	 */
	public abstract Double getValue(QuantizableMultiDimProcessInterface model) throws IllegalArgumentException;

}
