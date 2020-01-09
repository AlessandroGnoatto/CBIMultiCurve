package net.finmath.stochastic;

import java.util.function.DoubleUnaryOperator;
import java.util.function.UnaryOperator;

import org.apache.commons.math3.complex.Complex;

import net.finmath.timeseries.*;

/**
 * This interface has to be implemented by each (multi-dimensional) CBI process (or class of CBI ones like the flow of tempered CBI processes), 
 * which we are going to use within this project. 
 *
 * @author Szulda Guillaume
 */
public interface CBIProcessInterface {
	
	public double getTimeHorizon();
	
	public int getNumberOfTimeSteps();
	
	public int getNumberOfParameters();
	
	public int getDimension();
	
	public double[] getInitialValues();
    
    public CBIProcessInterface getCloneForModifiedParameters(double[] parameters);
	
	public double[] getImmigrationRates();
	
	public double[] getLambda();
		
	public double[] getParameterLowerBounds();
	
	public double[] getParameterUpperBounds();
	
	public double[] getParameters();

	public DoubleUnaryOperator getBranchingMechanism();
	
	public UnaryOperator<Complex> getComplexBranchingMechanism();
	
	public FunctionVZero[] getFunctionsVZero();
	
	public FunctionVMinusOne[] getFunctionsVMinusOne();
	
	public FunctionW[] getFunctionsW(Complex[] u);

	
	
}
