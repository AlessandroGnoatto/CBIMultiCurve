package net.finmath.montecarlo.models;

import java.util.Map;

import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.*;
import net.finmath.montecarlo.process.*;
import net.finmath.stochastic.*;

/**
 * This interface has to be implemented by a class that is intended to represent a Monte Carlo simulation of a multiple yield curve model,
 * then it will have as an attribute the Monte Carlo simulation of its driving process (here a single CBI process or several like a flow),
 * which will be used to compute some data that usually characterize a multiple yield curve model (ZC bonds, multiplicative spreads),
 * as well as the usual features that an interest rate model must have at disposal (the initial term structure for instance).
 * Note that the numbering of the corresponding set of tenors starts from the 0-th tenor (first tenor, i=0) to the last one (i=getDimension()-1),
 * this way, getForwardCurve(int i) provides the curve associated to the i-th tenor and getSpreadValue(double time, int j) computes the spread value,
 * of the i-th tenor at time. Similar methods are available when ones has the name of the tenor in question instead of its index in the array.
 * @author Szulda Guillaume
 *
 */
public interface MonteCarloCBIDrivenMultiCurveInterface extends MonteCarloSimulationInterface {
	
	public MonteCarloCBIProcessInterface getMonteCarloCBIProcess();
	
	public double getTimeHorizon();
	
	public int getNumberOfTimeSteps();
	
	public int getDimension();
	
	public double getTenorLength(String tenorName);
	
	public AnalyticModelInterface getAnalyticModel();
	
	public Map<String,CurveInterface> getInitialCurves();
	
	public DiscountCurveInterface getDiscountCurve();
	
	public ForwardCurveInterface getForwardCurve(String tenorName);
	
	public MonteCarloCBIDrivenMultiCurveInterface getCloneWithModifiedSeed(int seed);
	
	public RandomVariableInterface getSpreadValue(double time, String tenorName);
	
	public RandomVariableInterface getZCBond(double time, double maturity);
	
    public RandomVariableInterface getNumeraire(double time);
	
}
