package net.finmath.stochastic;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;
import java.util.function.UnaryOperator;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.complex.Complex;

import net.finmath.fouriermethod.calibration.*;
import net.finmath.timeseries.*;

/**
 * This class stands for a particular class of CBI processes, namely a flow of tempered alpha-stable CBI processes.
 * It also contains the constraints that its parameters have to satisfy for the corresponding multi-curve model to be admissible.
 * The user can also create an instance of it while choosing which parameter has to be calibrated or not.
 * 
 * @author Szulda Guillaume
 */
public class FlowOfTemperedAlphaStableCBIprocess implements CBIProcessInterface {
	
	private double timeHorizon;
	private int numberOfTimeSteps;
	private double[] lambda;
	private double[] immigrationRates;
	private double b;
	private double sigma;
	private double eta;
	private double zeta;
	private double alpha;
	private double[] initialValues;
	private DoubleUnaryOperator psi;
	private UnaryOperator<Complex> cpsi;
	private FunctionVZero[] functionsVZero;
	private FunctionVMinusOne[] functionsVMinusOne;
	
	private ScalarParameterInformationInterface[] lambdaInfo;
	private ScalarParameterInformationInterface[] immigrationRatesInfo;
	private ScalarParameterInformationInterface bInfo;
	private ScalarParameterInformationInterface sigmaInfo;
	private ScalarParameterInformationInterface etaInfo;
	private ScalarParameterInformationInterface zetaInfo;
	private ScalarParameterInformationInterface alphaInfo;
	private ScalarParameterInformationInterface[] initialValuesInfo;
	private boolean functionVConstraint;
	private boolean expMomentConstraint;
	
	/*
	 * Upper and lower bounds are collected here for convenience:
	 * such vectors are then passed to the factory of the optimization algorithm.
	 * In this way we guarantee consistency between the constraints in the model
	 * and the constraints in the optimizer factory.
	 */
	private double[] parameterLowerBounds;
	private double[] parameterUpperBounds;
	
	/**
	 * This constructor creates a flow of tempered alpha-stable CBI processes according to the predetermined specification,
	 * which is by means of the following parameters associated to the usual constraints for the model/flow to be admissible :
	 * These are intrinsic to the flow itself (which have to be calibrated):
	 * @param initialValues (Vector)
	 * @param immigrationRates (Vector)
	 * @param b
	 * @param sigma
	 * @param eta
	 * @param zeta
	 * @param alpha
	 * But the following input does not belong to the flow but is required here anyhow to link the flow to its corresponding multi-curve model,
	 * it will have to be calibrated as well:
	 * @param lambda (Vector)
	 * @throws IllegalArgumentException
	 */
	public FlowOfTemperedAlphaStableCBIprocess(double timeHorizon, int numberOfTimeSteps, double[] initialValues, double[] immigrationRates, double b, double sigma, double eta, double zeta, double alpha, double[] lambda) throws IllegalArgumentException {
		
		if(initialValues.length != immigrationRates.length || initialValues.length != lambda.length) {
			
			throw new IllegalArgumentException("initialValues, immigrationRates and lambda must have the same size (the dimension of the flow).");
		
		} else {
			
			this.timeHorizon = timeHorizon;
			this.numberOfTimeSteps = numberOfTimeSteps;
			this.lambda = lambda;
			this.alpha = alpha;
			this.b = b;
			this.zeta = zeta;
			this.eta = eta;
			this.sigma = sigma;
				
			this.immigrationRates = immigrationRates;
			this.initialValues = initialValues;
			
			/* The next one will determine the information on the parameters that was predetermined for this specification of the multi-curve model that will be used for calibration,
			 * which consists of the constraints that make the flow/model admissible and whether one parameter has to be calibrated or not. */
			generateInformationForParameters();
			this.parameterLowerBounds = extractLowerBounds();
			this.parameterUpperBounds = extractUpperBounds();
			
			this.functionVConstraint = true;
			this.expMomentConstraint = true;
			
			if(expMomentConstraint && this.zeta < this.eta) {
				this.zeta = this.eta + 1E-9;
			}
			
			if(functionVConstraint && this.b < this.sigma*this.sigma*(this.zeta / this.eta) - ((this.alpha*this.eta*Math.pow(this.zeta, this.alpha-1))/Math.cos(0.5*this.alpha*Math.PI))) {
				this.b = this.sigma*this.sigma*(this.zeta / this.eta) - ((this.alpha*this.eta*Math.pow(this.zeta, this.alpha-1))/Math.cos(0.5*this.alpha*Math.PI)) + 1E-9;
			}
			
			//Computation of the branching mechanism of the flow (/psi) along with its complex counterpart:
			this.psi = x -> this.b*x + 0.5*Math.pow(this.sigma*x, 2) + ((Math.pow(this.zeta, this.alpha) + x*this.eta*this.alpha*Math.pow(this.zeta, this.alpha-1) - Math.pow(x*this.eta + this.zeta, this.alpha)) / Math.cos(Math.PI*this.alpha*0.5));
			this.cpsi = x -> {
			Complex a = x.multiply(this.b);
			Complex r = (x.multiply(x)).multiply(0.5*Math.pow(this.sigma, 2));
			Complex l = (x.multiply(this.eta * this.alpha / this.zeta).add(1)).multiply(Math.pow(this.zeta, this.alpha));
			Complex c = ((x.multiply(this.alpha*this.eta*Math.pow(this.zeta,this.alpha-1))).add(Math.pow(this.zeta,this.alpha))).subtract(l);
			return (a.add(r)).add(c.divide(Math.cos(Math.PI*0.5*this.alpha)));};
				
			this.functionsVZero = new FunctionVZero[this.lambda.length];
			this.functionsVMinusOne = new FunctionVMinusOne[this.lambda.length];
			
			for(int i = 0; i < lambda.length; i++) {
				functionsVZero[i] = new FunctionVZero(this.timeHorizon, this.numberOfTimeSteps, this.lambda[i], this.psi);
				functionsVMinusOne[i] = new FunctionVMinusOne(this.timeHorizon, this.numberOfTimeSteps, this.lambda[i], this.psi);
			}
			
			
		}
		
	}
	
	/**
	 * This constructor creates a flow of tempered alpha-stable CBI processes with more general information on parameters,
	 * which means that some of the used constraints can keep the model/flow from being theoretically admissible.
	 * The care in regard to this fact and also to the parameters to calibrate or not is then left to the user when choosing the information.
	 * These are intrinsic to the flow itself:
	 * @param initialValues (Vector)
	 * @param immigrationRates (Vector)
	 * @param b
	 * @param sigma
	 * @param eta
	 * @param zeta
	 * @param alpha
	 * But the following does not belong to the flow but is required here anyhow to link the flow to its corresponding multi-curve model:
	 * @param lambda (Vector)
	 * And the next ones stand for the pieces of information (constraints) on each parameter of the flow/model :
	 * @param lambdaInfo
	 * @param immigrationRatesInfo
	 * @param bInfo
	 * @param sigmaInfo
	 * @param etaInfo
	 * @param zetaInfo
	 * @param alphaInfo
	 * @param initialValuesInfo
	 * @param functionVConstraint
	 * @param expMomentConstraint
	 * @throws IllegalArgumentException
	 */
	public FlowOfTemperedAlphaStableCBIprocess(double timeHorizon, int numberOfTimeSteps, double[] initialValues, double[] immigrationRates, double b, double sigma, 
			double eta, double zeta, double alpha, double[] lambda, 
			ScalarParameterInformationInterface[] lambdaInfo, ScalarParameterInformationInterface[] immigrationRatesInfo, 
			ScalarParameterInformationInterface bInfo, ScalarParameterInformationInterface sigmaInfo, ScalarParameterInformationInterface etaInfo, 
			ScalarParameterInformationInterface zetaInfo, ScalarParameterInformationInterface alphaInfo, ScalarParameterInformationInterface[] initialValuesInfo,
			boolean functionVConstraint, boolean expMomentConstraint) throws IllegalArgumentException {
		
		if(initialValues.length != immigrationRates.length || initialValues.length != lambda.length) {
			
			throw new IllegalArgumentException("initialValues, immigrationRates and lambda must have the same size (dimension of the flow).");
		
		} else {
			
			this.timeHorizon = timeHorizon;
			this.numberOfTimeSteps = numberOfTimeSteps;
			this.lambda = lambda;
				
			this.alpha = alpha;
			this.b = b;
			this.zeta = zeta;
			this.eta = eta;
			this.sigma = sigma;
				
			this.immigrationRates = immigrationRates;
			this.initialValues = initialValues;
			
			this.lambdaInfo = lambdaInfo;
			this.immigrationRatesInfo = immigrationRatesInfo;
			this.bInfo = bInfo;
			this.sigmaInfo = sigmaInfo;
			this.etaInfo = etaInfo;
			this.zetaInfo = zetaInfo;
			this.alphaInfo = alphaInfo;
			this.initialValuesInfo = initialValuesInfo;
			this.parameterLowerBounds = extractLowerBounds();
			this.parameterUpperBounds = extractUpperBounds();
			this.functionVConstraint = functionVConstraint;
			this.expMomentConstraint = expMomentConstraint;
			
			if(expMomentConstraint && this.zeta < this.eta) {
				this.zeta = this.eta + 1E-9;
			}
			
			if(functionVConstraint && this.b < this.sigma*this.sigma*(this.zeta / this.eta) - ((this.alpha*this.eta*Math.pow(this.zeta, this.alpha-1))/Math.cos(0.5*this.alpha*Math.PI))) {
				this.b = this.sigma*this.sigma*(this.zeta / this.eta) - ((this.alpha*this.eta*Math.pow(this.zeta, this.alpha-1))/Math.cos(0.5*this.alpha*Math.PI)) + 1E-9;
			}
			
			//Computation of the branching mechanism of the flow (/psi) along with its complex counterpart:
			this.psi = x -> this.b*x + 0.5*Math.pow(this.sigma*x, 2) + ((Math.pow(this.zeta, this.alpha) + x*this.eta*this.alpha*Math.pow(this.zeta, this.alpha-1) - Math.pow(x*this.eta + this.zeta, this.alpha)) / Math.cos(Math.PI*this.alpha*0.5));
			this.cpsi = x -> {
				Complex a = x.multiply(this.b);
				Complex r = (x.multiply(x)).multiply(0.5*Math.pow(this.sigma, 2));
				Complex l = (x.multiply(this.eta * this.alpha / this.zeta).add(1)).multiply(Math.pow(this.zeta, this.alpha));
				Complex c = ((x.multiply(this.alpha*this.eta*Math.pow(this.zeta,this.alpha-1))).add(Math.pow(this.zeta,this.alpha))).subtract(l);
				return (a.add(r)).add(c.divide(Math.cos(Math.PI*0.5*this.alpha)));};
				
			this.functionsVZero = new FunctionVZero[this.lambda.length];
			this.functionsVMinusOne = new FunctionVMinusOne[this.lambda.length];
			
			for(int i = 0; i < lambda.length; i++) {
				functionsVZero[i] = new FunctionVZero(this.timeHorizon, this.numberOfTimeSteps, this.lambda[i], this.psi);
				functionsVMinusOne[i] = new FunctionVMinusOne(this.timeHorizon, this.numberOfTimeSteps, this.lambda[i], this.psi);
			}
			
		}
		
	}
	
	@Override
	public double getTimeHorizon() {
		return this.timeHorizon;
	}
	
	@Override 
	public int getNumberOfTimeSteps() {
		return this.numberOfTimeSteps;
	}
	
	@Override
	public int getNumberOfParameters() {
		return 5 + (this.immigrationRates).length + (this.initialValues).length + (this.lambda).length;
	}
	
	@Override
	public int getDimension() {
		return (this.immigrationRates).length;
	}
	
	@Override
    public double[] getInitialValues() {
    	return this.initialValues;
    }
	
	@Override
	public double[] getImmigrationRates() {
		return this.immigrationRates;
	}
	
	@Override
	public double[] getLambda() {
		return this.lambda;
	}
	
	@Override
	public DoubleUnaryOperator getBranchingMechanism() {
		return this.psi;
	}
	
	@Override
	public UnaryOperator<Complex> getComplexBranchingMechanism() {
		return this.cpsi;
	}
	
	@Override
	public FunctionVZero[] getFunctionsVZero() {
		return this.functionsVZero;
	}
	
	@Override
	public FunctionVMinusOne[] getFunctionsVMinusOne() {
		return this.functionsVMinusOne;
	}
	
	@Override
    public FunctionW[] getFunctionsW(Complex[] u) throws IllegalArgumentException {
		if(u.length != (this.lambda).length) {
			throw new IllegalArgumentException("The complex argument and lambda must have the same length.");
		} else {
			FunctionW[] functionsW = new FunctionW[u.length];
			for(int i = 0; i < (this.lambda).length; i++) {
				functionsW[i] = new FunctionW(this.timeHorizon, this.numberOfTimeSteps, this.lambda[i], this.cpsi, u[i]);
			}
			return functionsW;
		}
    }
	
	@Override
	public CBIProcessInterface getCloneForModifiedParameters(double[] parameters) {
		
		/* For each parameter, we check whether it has to be calibrated or not. If so, we replace it for the new one to which we apply the corresponding constraint.
		 * If not, this parameter is not modified.
		 */
		double newB = this.bInfo.getIsParameterToCalibrate() == true ? this.bInfo.getConstraint().applyConstraint(parameters[0]) : this.b;
		double newSigma = this.sigmaInfo.getIsParameterToCalibrate() == true ? this.sigmaInfo.getConstraint().applyConstraint(parameters[1]) : this.sigma;
		double newEta = this.etaInfo.getIsParameterToCalibrate() == true ? this.etaInfo.getConstraint().applyConstraint(parameters[2]) : this.eta;
		double newZeta = this.zetaInfo.getIsParameterToCalibrate() == true ? this.zetaInfo.getConstraint().applyConstraint(parameters[3]) : this.zeta;
		double newAlpha = this.alphaInfo.getIsParameterToCalibrate() == true ? this.alphaInfo.getConstraint().applyConstraint(parameters[4]) : this.alpha;
		
		double[] newInitialValues = new double[this.getDimension()];
		double[] newImmigrationRates = new double[this.getDimension()];
		double[] newLambda = new double[this.getDimension()];
		
		for(int i = 0; i < this.getDimension(); i++) {
			newInitialValues[i] = (this.initialValuesInfo[i]).getIsParameterToCalibrate() == true ? (this.initialValuesInfo[i]).getConstraint().applyConstraint(parameters[i+5]) : this.initialValues[i];
			newImmigrationRates[i] = (this.immigrationRatesInfo[i]).getIsParameterToCalibrate() == true ? (this.immigrationRatesInfo[i]).getConstraint().applyConstraint(parameters[i+5+(this.initialValues).length]) : this.immigrationRates[i];
			newLambda[i] = (this.lambdaInfo[i]).getIsParameterToCalibrate() == true ? (this.lambdaInfo[i]).getConstraint().applyConstraint(parameters[i+5+(this.initialValues).length+(this.immigrationRates).length]) : this.lambda[i];
		}
		
		if(this.expMomentConstraint && newZeta < newEta) {
			newZeta = newEta + 1E-2;
		}
		
		if(this.functionVConstraint && newB < newSigma*newSigma*(newZeta / newEta) - ((newAlpha*newEta*Math.pow(newZeta, newAlpha-1))/Math.cos(0.5*newAlpha*Math.PI))) {
			newB = newSigma*newSigma*(newZeta / newEta) - ((newAlpha*newEta*Math.pow(newZeta, newAlpha-1))/Math.cos(0.5*newAlpha*Math.PI)) + 1E-2;
		}
		
		return new FlowOfTemperedAlphaStableCBIprocess(this.timeHorizon, this.numberOfTimeSteps, newInitialValues, newImmigrationRates, newB, newSigma, newEta, newZeta, newAlpha, newLambda, this.lambdaInfo, this.immigrationRatesInfo, this.bInfo, this.sigmaInfo, this.etaInfo, this.zetaInfo, this.alphaInfo, this.initialValuesInfo, this.functionVConstraint, this.expMomentConstraint);
	
	}
	
	public double getB() {
		return this.b;
	}
	
	public double getSigma() {
		return this.sigma;
	}
	
	public double getEta() {
		return this.eta;
	}
	
	public double getZeta() {
		return this.zeta;
	}
	
	public double getAlpha() {
		return this.alpha;
	}
	
	@Override
	public double[] getParameterLowerBounds() {
		return parameterLowerBounds;
	}
	
	@Override
	public double[] getParameterUpperBounds() {
		return parameterUpperBounds;
	}
	
	/**
	 * This method allows to impose the usual constraints on the parameters of the flow/model that make the corresponding model to be admissible.
	 * It also informs whether one parameter or another has to be calibrated or not.
	 **/
	private void generateInformationForParameters() {
		
		this.bInfo = new ScalarParameterInformation(true, new PositivityConstraint());
		this.sigmaInfo = new ScalarParameterInformation(true, new PositivityConstraint());
		this.etaInfo = new ScalarParameterInformation(true, new PositivityConstraint());
		this.zetaInfo = new ScalarParameterInformation(true, new PositivityConstraint());
		this.alphaInfo = new ScalarParameterInformation(true, new BoundConstraint(1, 2));
		this.initialValuesInfo = new ScalarParameterInformationInterface[this.getDimension()];
		this.immigrationRatesInfo = new ScalarParameterInformationInterface[this.getDimension()];
		this.lambdaInfo = new ScalarParameterInformationInterface[this.getDimension()];
		for(int i = 0; i < this.getDimension(); i++) {
			this.initialValuesInfo[i] = new ScalarParameterInformation(true, new BoundConstraint(1E-4,1.0));//new PositivityConstraint());
			this.immigrationRatesInfo[i] = new ScalarParameterInformation(true, new BoundConstraint(1E-2,1.0));//new PositivityConstraint());
			this.lambdaInfo[i] = new ScalarParameterInformation(false, new PositivityConstraint()); 
		}
	
	}
	
	private double[] extractUpperBounds() {
		
		double[] upperBounds = new double[5+(this.initialValues).length+(this.immigrationRates).length+(this.lambda).length];
		double threshold = 1E6;
		upperBounds[0] = this.bInfo.getConstraint().getUpperBound() > threshold ? threshold : this.bInfo.getConstraint().getUpperBound();
		upperBounds[1] = this.sigmaInfo.getConstraint().getUpperBound() > threshold ? threshold : this.sigmaInfo.getConstraint().getUpperBound();
		upperBounds[2] = this.etaInfo.getConstraint().getUpperBound() > threshold ? threshold : this.etaInfo.getConstraint().getUpperBound();
		upperBounds[3] = this.zetaInfo.getConstraint().getUpperBound() > threshold ? threshold : this.zetaInfo.getConstraint().getUpperBound();
		upperBounds[4] = this.alphaInfo.getConstraint().getUpperBound() > threshold ? threshold : this.alphaInfo.getConstraint().getUpperBound();
		for(int i = 0; i < this.getDimension(); i++) {
			upperBounds[i+5] = (this.initialValuesInfo[i]).getConstraint().getUpperBound() > threshold ? threshold : (this.initialValuesInfo[i]).getConstraint().getUpperBound();
			upperBounds[i+5+(this.initialValues).length] = (this.immigrationRatesInfo[i]).getConstraint().getUpperBound() > threshold ? threshold : (this.immigrationRatesInfo[i]).getConstraint().getUpperBound();
			upperBounds[i+5+(this.initialValues).length+(this.immigrationRates).length] = (this.lambdaInfo[i]).getConstraint().getUpperBound() > threshold ? threshold : (this.lambdaInfo[i]).getConstraint().getUpperBound();
		}
		return upperBounds;
		
	}

	private double[] extractLowerBounds() {

		double[] lowerBounds = new double[5+(this.initialValues).length+(this.immigrationRates).length+(this.lambda).length];
		double threshold = -1E6;
		lowerBounds[0] = this.bInfo.getConstraint().getLowerBound() < threshold ? threshold : this.bInfo.getConstraint().getLowerBound();
		lowerBounds[1] = this.sigmaInfo.getConstraint().getLowerBound() < threshold ? threshold : this.sigmaInfo.getConstraint().getLowerBound();
		lowerBounds[2] = this.etaInfo.getConstraint().getLowerBound() < threshold ? threshold : this.etaInfo.getConstraint().getLowerBound();
		lowerBounds[3] = this.zetaInfo.getConstraint().getLowerBound() < threshold ? threshold : this.zetaInfo.getConstraint().getLowerBound();
		lowerBounds[4] = this.alphaInfo.getConstraint().getLowerBound() < threshold ? threshold : this.alphaInfo.getConstraint().getLowerBound();
		for(int i = 0; i < this.getDimension(); i++) {
			lowerBounds[i+5] = (this.initialValuesInfo[i]).getConstraint().getLowerBound() < threshold ? threshold : (this.initialValuesInfo[i]).getConstraint().getLowerBound();
			lowerBounds[i+5+(this.initialValues).length] = (this.immigrationRatesInfo[i]).getConstraint().getLowerBound() < threshold ? threshold : (this.immigrationRatesInfo[i]).getConstraint().getLowerBound();
			lowerBounds[i+5+(this.initialValues).length+(this.immigrationRates).length] = (this.lambdaInfo[i]).getConstraint().getLowerBound() < threshold ? threshold : (this.lambdaInfo[i]).getConstraint().getLowerBound();
		}
		return lowerBounds;
		
	}
	
	public double[] getParameters() {
		  
		  List<Double> params = new ArrayList<Double>();
		  
		  int dimension = this.getDimension();
		  
		  params.add(this.getB());
		  params.add(this.getSigma());
		  params.add(this.getEta());
		  params.add(this.getZeta());
		  params.add(this.getAlpha());
		  
		  for(int i = 0; i<dimension; i++)
		    params.add(this.getInitialValues()[i]);
		  
		  for(int i = 0; i<dimension; i++)
		    params.add(this.getImmigrationRates()[i]);
		  
		  for(int i = 0; i<dimension; i++)
		    params.add(this.getLambda()[i]);
		  
		  Double[] array = params.toArray(new Double[params.size()]);
		  
		  return ArrayUtils.toPrimitive(array);
		}
	
}
