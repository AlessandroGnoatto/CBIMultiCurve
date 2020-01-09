package net.finmath.randomnumbers;

import java.io.Serializable;

public class MultiDimensionalMersenneTwister implements Serializable, RandomNumberGenerator {
	
	private static final long serialVersionUID = -1827470318370174186L;

	private final org.apache.commons.math3.random.MersenneTwister mersenneTwister;
	
	private final int dimension;

	public MultiDimensionalMersenneTwister(int seed, int dimension) {
		this.dimension = dimension;
		mersenneTwister	= new org.apache.commons.math3.random.MersenneTwister(seed);
	}

	public int getDimension() {
		return this.dimension;
	}
	
	public org.apache.commons.math3.random.RandomGenerator getOneDimMersenneTwister() {
		return this.mersenneTwister;
	}

	public double[] getNext() {
		double[] next = new double[dimension];
		for(int i = 0; i < dimension; i++) {
			next[i] = mersenneTwister.nextDouble();
		}
		return next;
	}

}
