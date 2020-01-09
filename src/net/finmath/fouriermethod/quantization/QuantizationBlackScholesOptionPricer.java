package net.finmath.fouriermethod.quantization;

import net.finmath.stochastic.RandomVariableInterface;

/**
 * B-S european call option pricer based on quantization.
 * Available thanks to the cubature formula approximating the time-0 call price by the expectation of the payoff function applied to
 * the voronoi quantization of the underlying Black-Scholes process at maturity.
 * This formula can be provided by the method call "getValue(QuantizableBlackScholesModel model)".
 * Its distribution is totally known given its companion weights over the corresponding quantization grid.
 * 
 * @author Szulda Guillaume
 */
public class QuantizationBlackScholesOptionPricer extends PricingByQuadraticQuantization {
	
	private final double strike;
	private final double maturity;
	
	public QuantizationBlackScholesOptionPricer(double strike, double maturity, String currency) {
		super(currency);
		this.strike = strike;
		this.maturity = maturity;
	}
	
	public QuantizationBlackScholesOptionPricer(double strike, double maturity) {
		super();
		this.strike = strike;
		this.maturity = maturity;
	}
	
	@Override
	public Double getValue(Quantizable1DProcessInterface model) throws IllegalArgumentException {
		if(model instanceof QuantizableBlackScholesModel) {
			try {
				return getValue((QuantizableBlackScholesModel)model);
			} catch (IllegalArgumentException e) {
				return null;
			}
		}
		else {
			throw new IllegalArgumentException("The product " + this.getClass()
			+ " cannot be valued against a model " + model.getClass() + "."
			+ "It requires a model of type " + QuantizableBlackScholesModel.class + ".");
		}
	}
	
	public Double getValue(QuantizableBlackScholesModel model) throws IllegalArgumentException {
		if(this.maturity != model.getMaturity()) {
			throw new IllegalArgumentException("The maturity of the payoff must be equal to the evaluation time at which the underlying process has been quantized.");
		} else {
			/* Declaration of the payoff of the product to price by means of the Voronoi quantization (QuantizableBlackScholesModel) of its underlying process (BlackScholesModel).*/ 
			RandomVariableInterface payoff = ((model.getVoronoiQuantization()).sub(strike)).floor(0);
			/* Use of the cubature formula to approximate the time-0 price of the option by means of the expectation of the payoff function of the quantization,
			 * whose distribution is known as a discrete law over the corresponding quantization grid with the associated companion weights.
			 * */
			return payoff.getAverage(model.getCompanionWeights())*model.getLevel()*Math.exp(-this.maturity*model.getRiskFreeRate());
		}
	}

	@Override
	public Double getValue(QuantizableMultiDimProcessInterface model) throws IllegalArgumentException {
		return null;
	}

}
