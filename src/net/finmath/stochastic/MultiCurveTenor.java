package net.finmath.stochastic;

/**
 * This class represents the concept of tenor in multi-curve interest rate modeling.
 * It simply defines it by means of its underlying name along with its length.
 * @author Szulda Guillaume.
 *
 */
public class MultiCurveTenor {
	
	private double tenor;
	private String name;
	
	public MultiCurveTenor(double tenor, String name) {
		this.tenor = tenor;
		this.name = name;
	}
	
	public double getTenorLength() {
		return this.tenor;
	}
	
	public String getTenorName() {
		return this.name;
	}

}
