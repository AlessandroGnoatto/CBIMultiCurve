package net.finmath.fouriermethod.quantization;

import net.finmath.stochastic.*;

/**
 * Multi curve caplet pricer based on quantization.
 * Available thanks to the cubature formula approximating the time-0 caplet price by the expectation of the payoff function applied to
 * the voronoi quantization of the underlying (QuantizableCBIDrivenMultiCurveModel) process at maturity.
 * This formula can be provided by the method call "getValue(QuantizableCBIDrivenMultiCurveModel model)".
 * Its distribution is totally known given its companion weights over the corresponding quantization grid.
 * 
 * @author Szulda Guillaume
 */
public class QuantizationMultiCurveCapletPricer extends PricingByQuadraticQuantization {
	
	private final double strike;
	private final double maturity;
	private final MultiCurveTenor tenor;
	
	public QuantizationMultiCurveCapletPricer(double strike, double maturity, String currency, MultiCurveTenor tenor) {
		super(currency);
		this.strike = strike;
		this.maturity = maturity;
		this.tenor = tenor;
	}
	
	public QuantizationMultiCurveCapletPricer(double strike, double maturity, MultiCurveTenor tenor) {
		super();
		this.strike = strike;
		this.maturity = maturity;
		this.tenor = tenor;
	}
	
	@Override
	public Double getValue(Quantizable1DProcessInterface model) throws IllegalArgumentException {
		return null;
	}
	
	@Override
	public Double getValue(QuantizableMultiDimProcessInterface model) throws IllegalArgumentException {
		if(model instanceof QuantizableCBIDrivenMultiCurveModel) {
			try {
				return getValue((QuantizableCBIDrivenMultiCurveModel)model);
			} catch (IllegalArgumentException e) {
				return null;
			}
		}
		else {
			throw new IllegalArgumentException("The product " + this.getClass()
			+ " cannot be valued against a model " + model.getClass() + "."
			+ "It requires a model of type " + QuantizableCBIDrivenMultiCurveModel.class + ".");
		}
	}
	
	public Double getValue(QuantizableCBIDrivenMultiCurveModel model) throws IllegalArgumentException {
		if(this.maturity != model.getMaturity() || this.tenor.getTenorLength() != model.getTenorLength() || this.tenor.getTenorName() != model.getTenorName()) {
			throw new IllegalArgumentException("The maturity along with the tenor of the payoff must be equal to the evaluation time at which the underlying process has been quantized and its associated tenor (respectively).");
		} else {
			/* Declaration of the payoff of the product to price by means of the Voronoi quantization (QuantizableCBIDrivenMultiCurveModel) of its underlying process (CBIDrivenMultiCurveModel).*/ 
			RandomVariableInterface payoff = ((model.getVoronoiQuantization()).sub(1+this.strike*model.getTenorLength())).floor(0);
			/* Use of the cubature formula to approximate the time-0 price of the caplet by means of the expectation of the payoff function of the quantization,
			 * whose distribution is known as a discrete law over the corresponding quantization grid with the associated companion weights.
			 * */
			return payoff.getAverage(model.getCompanionWeights())*model.getLevel()*(model.getDiscountCurve()).getDiscountFactor(this.maturity+model.getTenorLength());
		}
	}

}
