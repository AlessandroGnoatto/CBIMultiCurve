package net.finmath.fouriermethod.calibration.models;

import java.util.Map;
import java.util.function.*;
import java.lang.Math;

import org.apache.commons.math3.complex.Complex;

import net.finmath.marketdata.model.*;
import net.finmath.marketdata.model.curves.*;
import net.finmath.stochastic.*;
import net.finmath.timeseries.FunctionW;
import net.finmath.fouriermethod.CharacteristicFunctionInterface;
import net.finmath.fouriermethod.models.MultivariateProcessCharacteristicFunctionInterface;

/**
 * This class, implementing the MultivariateCalibrableProcessInterface interface, provides the characteristic function of a multiple yield curve model,
 * associated to a cbiProcess whose representing class implements the CBIProcessInterface interface. 
 * This CF can then be used for caplet pricing using FFT or other Fourier-based pricing methods towards model calibration.
 * This class also provides clones of itself for given parameters, while keeping on satisfying the associated constraints for the model to be admissible (or not).
 * It also informs whether a parameter or another has to be calibrated or not.
 * 
 * @author Szulda Guillaume
 */
public class CBIDrivenMultiCurveModel implements MultivariateCalibrableProcessInterface {
	
	private AnalyticModelInterface curves;
	private CBIProcessInterface cbiProcess;
	private DiscountCurveInterface initialDC;
	private MultiCurveTenor[] tenors;
	private ForwardCurveInterface[] initialFC;
	private DoubleBinaryOperator integralOfFunctionL;
	private DoubleUnaryOperator[] functionsC;
	
	/** 
	 * First constructor, creates an instance of the calibrable multi-curve model whose driving process is a CBI process.
	 * Here are the required parameters:
	 * @param curves
	 * @param cbiProcess
	 * @param tenors
	 * @throws IllegalArgumentException
	 */
	public CBIDrivenMultiCurveModel(AnalyticModelInterface curves, CBIProcessInterface cbiProcess, MultiCurveTenor[] tenors) throws IllegalArgumentException {
		if(cbiProcess.getDimension() != tenors.length) {
			throw new IllegalArgumentException("The dimension of the underlying CBI process and the number of tenors must be equal.");
		} else {
			this.curves = curves;
			this.cbiProcess = cbiProcess;
			this.initialDC = (this.curves).getDiscountCurve("discount-EUR-OIS");
			this.tenors = tenors;
			this.initialFC = new ForwardCurveInterface[(this.tenors).length];
			for(int i = 0; i < cbiProcess.getDimension(); i++) {
				if((this.tenors[i]).getTenorName() == "3M") {
					this.initialFC[i] = (this.curves).getForwardCurve("forward-EUR-3M");
				} else if((this.tenors[i]).getTenorName() == "6M") {
					this.initialFC[i] = (this.curves).getForwardCurve("forward-EUR-6M");
				} else {
					throw new IllegalArgumentException("The curve is not available");
				}
			}
			this.functionsC = new DoubleUnaryOperator[cbiProcess.getDimension()];
			achieveFitToInitialCurves();
		}
	}

	/** 
	 * Second constructor, creates an instance of the calibrable multi-curve model whose driving process is a CBI process.
	 * Here are the required parameters:
	 * @param curves
	 * @param cbiProcess
	 * @param tenorLengths
	 * @param tenorNames
	 * @throws IllegalArgumentException
	 */
	public CBIDrivenMultiCurveModel(AnalyticModelInterface curves, CBIProcessInterface cbiProcess, double[] tenorLengths, String[] tenorNames) throws IllegalArgumentException {
		if(cbiProcess.getDimension() != tenorLengths.length || cbiProcess.getDimension() != tenorNames.length) {
			throw new IllegalArgumentException("The dimension of the underlying CBI process and the number of tenors must be equal.");
		} else {
			this.curves = curves;
			this.cbiProcess = cbiProcess;
			this.initialDC = (this.curves).getDiscountCurve("discount-EUR-OIS");
			this.tenors = new MultiCurveTenor[cbiProcess.getDimension()];
			this.initialFC = new ForwardCurveInterface[this.tenors.length];
			for(int i = 0; i < cbiProcess.getDimension(); i++) {
				this.tenors[i] = new MultiCurveTenor(tenorLengths[i],tenorNames[i]);
				if((this.tenors[i]).getTenorName() == "3M") {
					this.initialFC[i] = (this.curves).getForwardCurve("forward-EUR-3M");
				} else if((this.tenors[i]).getTenorName() == "6M") {
					this.initialFC[i] = (this.curves).getForwardCurve("forward-EUR-6M");
				} else {
					throw new IllegalArgumentException("The curve is not available");
				}
			}
			this.functionsC = new DoubleUnaryOperator[cbiProcess.getDimension()];
			achieveFitToInitialCurves();
		}
	}

	/**
	 * Third constructor, creates an instance of the calibrable multi-curve model,
	 * whose driving process is a flow of tempered alpha-stable CBI processes associated to its usual information (constraints).
	 * Here are the required parameters: 
	 * @param timeHorizon
	 * @param curves
	 * @param tenorLengths
	 * @param tenorNames
	 * @param initialValues
	 * @param immigrationRates
	 * @param b
	 * @param sigma
	 * @param eta
	 * @param zeta
	 * @param alpha
	 * @param lambda
	 * @throws IllegalArgumentException
	 */
	public CBIDrivenMultiCurveModel(double timeHorizon, int numberOfTimeSteps, AnalyticModelInterface curves, double[] tenorLengths, String[] tenorNames, double[] initialValues, double[] immigrationRates, double b, double sigma, double eta, double zeta, double alpha, double[] lambda) throws IllegalArgumentException {
		this.cbiProcess = new FlowOfTemperedAlphaStableCBIprocess(timeHorizon, numberOfTimeSteps, initialValues, immigrationRates, b, sigma, eta, zeta, alpha, lambda);
		if((this.cbiProcess).getDimension() != tenorNames.length || (this.cbiProcess).getDimension() != tenorLengths.length) {
			throw new IllegalArgumentException("The dimension of the underlying CBI process and the number of tenors must be equal.");
		} else {
			this.curves = curves;
			this.initialDC = (this.curves).getDiscountCurve("discount-EUR-OIS");
			this.tenors = new MultiCurveTenor[cbiProcess.getDimension()];
			this.initialFC = new ForwardCurveInterface[this.tenors.length];
			for(int i = 0; i < cbiProcess.getDimension(); i++) {
				this.tenors[i] = new MultiCurveTenor(tenorLengths[i],tenorNames[i]);
				if((this.tenors[i]).getTenorName() == "3M") {
					this.initialFC[i] = (this.curves).getForwardCurve("forward-EUR-3M");
				} else if((this.tenors[i]).getTenorName() == "6M") {
					this.initialFC[i] = (this.curves).getForwardCurve("forward-EUR-6M");
				} else {
					throw new IllegalArgumentException("The curve is not available");
				}
			}
			this.functionsC = new DoubleUnaryOperator[cbiProcess.getDimension()];
			achieveFitToInitialCurves();
		}
	}
	
	/**
	 * Fourth and ultimate constructor, creates an instance of the calibrable multi-curve model,
	 * whose driving process is a flow of tempered alpha-stable CBI processes associated to its usual information (constraints).
	 * Here are the required parameters: 
	 * @param timeHorizon
	 * @param curves
	 * @param tenors
	 * @param initialValues
	 * @param immigrationRates
	 * @param b
	 * @param sigma
	 * @param eta
	 * @param zeta
	 * @param alpha
	 * @param lambda
	 * @throws IllegalArgumentException
	 */
	public CBIDrivenMultiCurveModel(double timeHorizon, int numberOfTimeSteps, AnalyticModelInterface curves, MultiCurveTenor[] tenors, double[] initialValues, double[] immigrationRates, double b, double sigma, double eta, double zeta, double alpha, double[] lambda) throws IllegalArgumentException {
		this.cbiProcess = new FlowOfTemperedAlphaStableCBIprocess(timeHorizon, numberOfTimeSteps, initialValues, immigrationRates, b, sigma, eta, zeta, alpha, lambda);
		if((this.cbiProcess).getDimension() != tenors.length) {
			throw new IllegalArgumentException("The dimension of the underlying CBI process and the number of tenors must be equal.");
		} else {
			this.curves = curves;
			this.initialDC = (this.curves).getDiscountCurve("discount-EUR-OIS");
			this.tenors = tenors;
			this.initialFC = new ForwardCurveInterface[this.tenors.length];
			for(int i = 0; i < cbiProcess.getDimension(); i++) {
				if((this.tenors[i]).getTenorName() == "3M") {
					this.initialFC[i] = (this.curves).getForwardCurve("forward-EUR-3M");
				} else if((this.tenors[i]).getTenorName() == "6M") {
					this.initialFC[i] = (this.curves).getForwardCurve("forward-EUR-6M");
				} else {
					throw new IllegalArgumentException("The curve is not available");
				}
			}
			this.functionsC = new DoubleUnaryOperator[cbiProcess.getDimension()];
			achieveFitToInitialCurves();
		}
	}
	
	public double getTimeHorizon() {
		return this.getCBIProcess().getTimeHorizon();
	}
	
	public int getNumberOfTimeSteps() {
		return this.getCBIProcess().getNumberOfTimeSteps();
	}
	
	public CBIProcessInterface getCBIProcess() {
		return this.cbiProcess;
	}
	
	public int getDimension() {
		return (this.cbiProcess).getDimension();
	}
	
    public double getTenorLength(int i) {
    	return (this.tenors[i]).getTenorLength();
    }
    
    public double getTenorLength(String tenorName) throws IllegalArgumentException {

		int tenor;
		
		if(tenorName.equals("forward-EUR-3M")) {
			tenor = 0;
		}else if(tenorName.equals("forward-EUR-6M")){
			tenor = 1;
		}else {
			throw new IllegalArgumentException("The curve is not available");
		}
		
		return getTenorLength(tenor);
		 
    }
	
	public AnalyticModelInterface getAnalyticModel() {
		return this.curves;
	}
	
	public Map<String,CurveInterface> getInitialCurves() {
		return (this.curves).getCurves();
	}
	
	public DiscountCurveInterface getDiscountCurve() {
		return this.initialDC;
	}
	
	public ForwardCurveInterface getForwardCurve(int i) {
		return this.initialFC[i];
	}
	
	public ForwardCurveInterface getForwardCurve(String tenorName) throws IllegalArgumentException{

		int tenor;
		
		if(tenorName.equals("forward-EUR-3M")) {
			tenor = 0;
		}else if(tenorName.equals("forward-EUR-6M")){
			tenor = 1;
		}else {
			throw new IllegalArgumentException("The curve is not available");
		}
		
		return getForwardCurve(tenor);
		
	}
	
	
	/**
	 * This computes the CF of the model at some maturity for the tenor corresponding to underlying.
	 * @param maturity
	 * @return
	 */
	@Override
	public CharacteristicFunctionInterface apply(double maturity, String underlying) throws IllegalArgumentException {
		
		int tenor;
		
		if(underlying.equals("forward-EUR-3M")) {
			tenor = 0;
		}else if(underlying.equals("forward-EUR-6M")){
			tenor = 1;
		}else {
			throw new IllegalArgumentException("The curve is not available");
		}
		
		return applyForSomeTenor(maturity, tenor);
		
	}
	
	/**
	 * This computes the CF of the model for the t-th tenor at some maturity (0 <= t <= getDimension()-1).
	 * @param maturity
	 * @param t
	 * @return
	 */
	public CharacteristicFunctionInterface applyForSomeTenor(double maturity, int t) throws IllegalArgumentException {
		if(maturity + this.getTenorLength(t) <= this.getTimeHorizon()) {
			return new CharacteristicFunctionInterface() {
				@Override
				public Complex apply(Complex w) {
					double sum1 = 0;
					Complex[] u = new Complex[getCBIProcess().getDimension()];
					for(int i = 0; i < getCBIProcess().getDimension(); i++) {
						if(i <= t) {
							u[i] = ((Complex.I.multiply(w).subtract(1)).multiply(getCBIProcess().getFunctionsVZero()[i].getValue(getTenorLength(t)))).add(Complex.I.multiply(w));
						} else if(i > t) {
							u[i] = (Complex.I.multiply(w).subtract(1)).multiply(getCBIProcess().getFunctionsVZero()[i].getValue(getTenorLength(t)));
						}
						sum1 = sum1 + getCBIProcess().getImmigrationRates()[i]*getCBIProcess().getFunctionsVZero()[i].getIntegral(0, getTenorLength(t));
					}
					Complex sum2 = new Complex(0,0);
					FunctionW[] function = getCBIProcess().getFunctionsW(u);
					for(int j = 0; j < getCBIProcess().getDimension(); j++) {
						sum2 = sum2.add((function[j].getValue(maturity).multiply(getCBIProcess().getInitialValues()[j])).add(function[j].getIntegral(0, maturity).multiply(getCBIProcess().getImmigrationRates()[j])));
					}
					return (Complex.I.multiply(w).multiply(sum1 + integralOfFunctionL.applyAsDouble(maturity, maturity + getTenorLength(t)) + functionsC[t].applyAsDouble(maturity))).add(-sum1 - integralOfFunctionL.applyAsDouble(0, maturity + getTenorLength(t))).exp().multiply(sum2.multiply(-1).exp());
				}
			};
		} else {
			throw new IllegalArgumentException("The time at which the CF is considered must be inside the validity domain of the CBI process.");
		}
	}

	@Override
	public MultivariateProcessCharacteristicFunctionInterface getCharacteristiFunction() {
		return this;
	}

	@Override
	public double[] getParameterLowerBounds() {
		return (this.cbiProcess).getParameterLowerBounds();
	}
	
	@Override
	public double[] getParameterUpperBounds() {
		return (this.cbiProcess).getParameterUpperBounds();
	}
	
	@Override
	public CBIDrivenMultiCurveModel getCloneForModifiedParameters(double[] parameters) {
		return new CBIDrivenMultiCurveModel(this.curves, (this.cbiProcess).getCloneForModifiedParameters(parameters), this.tenors);
	}
	
	private void achieveFitToInitialCurves() {
		for(int k = 0; k < this.getDimension(); k++) {
			int i = k;
			this.functionsC[i] = t -> {
				double a = Math.log(1 + this.tenors[i].getTenorLength()*this.initialFC[i].getForward(this.getAnalyticModel(), t)) + 
						Math.log(this.initialDC.getDiscountFactor(t + this.tenors[i].getTenorLength())) - 
						Math.log(this.initialDC.getDiscountFactor(t));
				double sum = 0;
				for(int j = 0; j <= i; j++) {
					sum = sum + 
							this.cbiProcess.getInitialValues()[j]*(this.cbiProcess.getFunctionsVMinusOne()[j].getValue(t) - this.cbiProcess.getFunctionsVZero()[j].getValue(t)) +
							this.cbiProcess.getImmigrationRates()[j]*(this.cbiProcess.getFunctionsVMinusOne()[j].getIntegral(0, t) - this.cbiProcess.getFunctionsVZero()[j].getIntegral(0, t));
				}
				return a + sum;
			};
		}
		this.integralOfFunctionL = (a,b) -> {
			double x = Math.log(this.initialDC.getDiscountFactor(a)) - Math.log(this.initialDC.getDiscountFactor(b));
			double sum = 0;
			for(int i = 0; i < this.getDimension(); i ++) {
				sum = sum + this.cbiProcess.getImmigrationRates()[i]*this.cbiProcess.getFunctionsVZero()[i].getIntegral(a, b) +
						this.cbiProcess.getInitialValues()[i]*(this.cbiProcess.getFunctionsVZero()[i].getValue(b) - this.cbiProcess.getFunctionsVZero()[i].getValue(a)); 
			}
			return x - sum;
		};
	}
	
	@Override
	public double[] getParameters() {
		return this.cbiProcess.getParameters();
	}

}

