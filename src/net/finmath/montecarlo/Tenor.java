package net.finmath.montecarlo;

public class Tenor {
	
	private double tenor;
	private String name;
	
	public Tenor(double tenor, String name) {
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
