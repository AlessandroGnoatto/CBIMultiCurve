/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.products;

import java.util.HashMap;
import java.util.Map;

import net.finmath.modelling.ModelInterface;
import net.finmath.modelling.ProductInterface;
import net.finmath.montecarlo.models.MonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Base class for products requiring an MonteCarloSimulationInterface for valuation.
 *
 * @author Christian Fries
 */
public abstract class AbstractMonteCarloProduct implements ProductInterface {

	private final String currency;

	public AbstractMonteCarloProduct(String currency) {
		super();
		this.currency = currency;
	}

	public AbstractMonteCarloProduct() {
		this(null);
	}

	@Override
	public Object getValue(ModelInterface model) throws IllegalArgumentException {
		if(model instanceof MonteCarloSimulationInterface) {
			return getValue((MonteCarloSimulationInterface)model);
		} else {
			throw new IllegalArgumentException("The product " + this.getClass()
			+ " cannot be valued against a model " + model.getClass() + "."
			+ "It requires a model of type " + MonteCarloSimulationInterface.class + ".");
		}
	}

	/**
	 * This method returns the random variable value of the product within the specified model.
	 *
	 * @param model The model used to price the product.
	 * @return The time-0 representing the value of the product.
	 */
	public abstract RandomVariableInterface getValue(MonteCarloSimulationInterface model);
	
	/**
	 * This method returns the time-0 price of the product within the specified model.
	 *
	 * @param model The model used to price the product.
	 * @return The time-0 representing the value of the product.
	 */
	public double getPrice(MonteCarloSimulationInterface model) {
		return getValue(model).getAverage();
	}

	/**
	 * This method returns the value of the product under the specified model and other information in a key-value map.
	 *
	 * @param model A model used to evaluate the product.
	 * @return The values of the product.
	 */
	public Map<String, Object> getValues(MonteCarloSimulationInterface model) {
		
		RandomVariableInterface values = getValue(model);

		if(values == null) {
			return null;
		}

		// Sum up values on path
		double value = values.getAverage();
		double error = values.getStandardError();

		Map<String, Object> results = new HashMap<>();
		results.put("value", value);
		results.put("error", error);

		return results;
	}

	/**
	 * This method returns the value under shifted market data (or model parameters).
	 * In its default implementation it does bump (creating a new model) and revalue.
	 * Override the way the new model is created, to implemented improved techniques (proxy scheme, re-calibration).
	 *
	 * @param model The model used to price the product, except for the market data to modify
	 * @param dataModified The new market data object to use (could be of different types)
	 *
	 * @return The values of the product.
	 */
	public Map<String, Object> getValuesForModifiedData(MonteCarloSimulationInterface model, Map<String,Object> dataModified) {
		
		MonteCarloSimulationInterface modelModified = model.getCloneWithModifiedData(dataModified);

		return getValues(modelModified);
	}

	/**
	 * This method returns the value under shifted market data (or model parameters).
	 * In its default implementation it does bump (creating a new model) and revalue.
	 * Override the way the new model is created, to implemented improved techniques (proxy scheme, re-calibration).
	 *
	 * @param model The model used to price the product, except for the market data to modify
	 * @param entityKey The entity to change, it depends on the model if the model reacts to this key.
	 * @param dataModified The new market data object to use (could be of different types)
	 *
	 * @return The values of the product.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public Map<String, Object> getValuesForModifiedData(MonteCarloSimulationInterface model, String entityKey, Object dataModified) {
		
		Map<String, Object> dataModifiedMap = new HashMap<>();
		dataModifiedMap.put(entityKey, dataModified);
		return getValuesForModifiedData(model, dataModifiedMap);
		
	}

	/**
	 * Returns the currency string of this notional.
	 *
	 * @return the currency
	 */
	public String getCurrency() {
		return currency;
	}

	@Override
	public String toString() {
		return "AbstractMonteCarloProduct [currency=" + currency + "]";
	}
}
