package net.finmath.montecarlo.products;

import net.finmath.montecarlo.models.*;
import net.finmath.stochastic.*;

/**
 * This class represents a caplet (interest rate derivative) in a multi-curve setting, 
 * which has to be priced by a Monte-Carlo simulation of a multiple yield curve model.
 * 
 * @author Szulda Guillaume
 */
public class MonteCarloMultiCurveCapletPricer extends AbstractMonteCarloProduct {
	
	private final double strike;
	private final double maturity;
	private final String tenorName;
	
	public MonteCarloMultiCurveCapletPricer(double strike, double maturity, String currency, String tenorName) {
		super(currency);
		this.strike = strike;
		this.maturity = maturity;
		this.tenorName = tenorName;
		
	}
	
	public MonteCarloMultiCurveCapletPricer(double strike, double maturity, String tenorName) {
		super();
		this.strike = strike;
		this.maturity = maturity;
		this.tenorName = tenorName;
	}
	
	@Override
	public RandomVariableInterface getValue(MonteCarloSimulationInterface model) throws IllegalArgumentException {
		if(model instanceof MonteCarloCBIDrivenMultiCurveInterface) {
			return getValue((MonteCarloCBIDrivenMultiCurveInterface)model);
		}
		else {
			throw new IllegalArgumentException("The product " + this.getClass()
			+ " cannot be valued against a model " + model.getClass() + "."
			+ "It requires a model of type " + MonteCarloCBIDrivenMultiCurveInterface.class + ".");
		}
	}
	
	/**
	 * This method provides the random value of a caplet seen in a multi-curve setting, whose used pricing method is 
	 * a Monte Carlo simulation of a interest rate multi-curve model whose driving process is a (or a family of) CBI process.
	 * 
	 * @param model
	 * @return Random value of the price of the caplet at evaluationTime
	 */
	public RandomVariableInterface getValue(MonteCarloCBIDrivenMultiCurveInterface model) {
		RandomVariableInterface payoff = (((model.getSpreadValue(maturity, tenorName)).sub((model.getZCBond(maturity, maturity+model.getTenorLength(tenorName))).mult(1+strike*model.getTenorLength(tenorName)))).floor(0)).div(model.getNumeraire(maturity));
		return ((payoff.mult(model.getMonteCarloWeights(maturity))).div(model.getMonteCarloWeights(0))).mult(model.getNumeraire(0));
	}

}
