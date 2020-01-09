package net.finmath.fouriermethod.products;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import net.finmath.exception.CalculationException;
import net.finmath.fouriermethod.CharacteristicFunctionInterface;
import net.finmath.fouriermethod.models.MultivariateProcessCharacteristicFunctionInterface;
import net.finmath.interpolation.RationalFunctionInterpolation;
import net.finmath.interpolation.RationalFunctionInterpolation.ExtrapolationMethod;
import net.finmath.interpolation.RationalFunctionInterpolation.InterpolationMethod;

public class CapletByCarrMadan extends EuropeanOptionSmileMultiAsset{
	//Fields
	private final int numberOfPoints;
	private final double gridSpacing;
	private final InterpolationMethod intMethod;
	private final ExtrapolationMethod extMethod;
	
	//Constructors
	public CapletByCarrMadan(String underlyingName, double maturity, double[] strikes) {
		super(underlyingName, maturity, strikes);
		this.numberOfPoints = 4096;
		this.gridSpacing = 0.1;
		this.intMethod =InterpolationMethod.HARMONIC_SPLINE;
		this.extMethod = ExtrapolationMethod.CONSTANT;
	}
		
	public CapletByCarrMadan(String underlyingName, double maturity, double[] strikes, int numberOfPoints,
			double gridSpacing, InterpolationMethod intMethod, ExtrapolationMethod extMethod) {
		super(underlyingName, maturity, strikes);
		this.numberOfPoints = numberOfPoints;
		this.gridSpacing = gridSpacing;
		this.intMethod = intMethod;
		this.extMethod = extMethod;
	}

	public Map<Double, Double> getValue(MultivariateProcessCharacteristicFunctionInterface model) throws CalculationException {

		double tenor = underlyingToTenor(getUnderlyingName());

		CharacteristicFunctionInterface modelCF = model.apply(getMaturity(), this.getUnderlyingName());

		final double lineOfIntegration = 0.5 * (getIntegrationDomainImagUpperBound()+getIntegrationDomainImagLowerBound());

		double lambda = 2*Math.PI/(numberOfPoints*gridSpacing); //Equation 23 Carr and Madan
		double upperBound = (numberOfPoints * lambda)/2.0; //Equation 20 Carr and Madan

		Complex[] integrandEvaluations = new Complex[numberOfPoints];

		for(int i = 0; i<numberOfPoints; i++) {

			double u = gridSpacing * i;

			//Integration over a line parallel to the real axis
			Complex z = new Complex(u,-lineOfIntegration);

			//The characteristic function is already discounted
			Complex numerator = modelCF.apply(z.subtract(Complex.I));

			Complex denominator = apply(z);
			Complex ratio = numerator.divide(denominator);
			ratio = (ratio.multiply(((Complex.I).multiply(upperBound*u)).exp())).multiply(gridSpacing);

			double delta;
			if (i==0){
				delta=1.0;
			}else{
				delta = 0.0;
			}	
			double simpsonWeight = (3+Math.pow(-1,i+1)-delta)/3;

			integrandEvaluations[i] = ratio.multiply(simpsonWeight);
		}

		//Compute the FFT
		Complex[] transformedVector = new Complex[numberOfPoints];
		FastFourierTransformer fft=new FastFourierTransformer(DftNormalization.STANDARD);
		transformedVector=fft.transform(integrandEvaluations,TransformType.FORWARD);

		//Find relevant prices via interpolation
		double[] logStrikeVector = new double[numberOfPoints];
		double[] strikeVector = new double[numberOfPoints];
		double[] optionPriceVector = new double[numberOfPoints];

		for(int j = 0; j<numberOfPoints; j++) {
		logStrikeVector[j] = -upperBound+lambda*j;
		strikeVector[j] = Math.exp(logStrikeVector[j]);
		optionPriceVector[j] = (transformedVector[j].multiply(Math.exp(-lineOfIntegration * logStrikeVector[j]))).getReal()/Math.PI;
		}

		RationalFunctionInterpolation interpolation = new RationalFunctionInterpolation(strikeVector, optionPriceVector,intMethod, extMethod);

		double[] strikes = getStrikes();

		int numberOfStrikes = strikes.length;
		HashMap<Double, Double> results = new HashMap<Double, Double>();

		Complex minusI = new Complex(0,-1);
		double residueTerm = (modelCF.apply(minusI)).getReal();

		for(int k = 0; k<numberOfStrikes; k++) {
			double myStrike = 1 + tenor*strikes[k];
			double kthPrice = Math.abs(residueTerm + interpolation.getValue(myStrike));
			results.put(strikes[k], kthPrice);
		}

		return results;
		
	}
	
	@Override
	public EuropeanOptionSmileMultiAsset getCloneWithModifiedParameters(String underlyingName, double maturity, double[] strikes) {
		return new CapletByCarrMadan(underlyingName, maturity, strikes);
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
