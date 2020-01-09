package net.finmath.fouriermethod.quantization;

import java.util.Arrays;
import java.util.stream.*;
import java.util.function.DoubleUnaryOperator;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.*;

import net.finmath.montecarlo.RandomVariable;
import net.finmath.stochastic.*;
import net.finmath.integration.TrapezoidalRealIntegrator;
import net.finmath.fouriermethod.calibration.models.BlackScholesModel;
import net.finmath.functions.ComplexSpecialFunctions;

/**
 * This class, implementing the Quantizable1DProcessInterface interface, deals with the quantization of a Black-Scholes model. 
 * Its underlying class, termed "BlackScholesModel", implements the CalibrableProcessInterface interface,
 * which provides the characteristic function known in closed form of the Black-Scholes model,
 * making pricing by quantization towards calibration possible.
 * 
 * @author Szulda Guillaume
 */
public class QuantizableBlackScholesModel implements Quantizable1DProcessInterface {
	
	private BlackScholesModel model;
	private double maturity;
	private int level;
	private double[] quantizer;
	private double[] companionWeights;
	private DoubleUnaryOperator f;
    
	/**
	 * First constructor, creates an instance of the QuantizableBlackScholesModel class, 
	 * thus quantizing at some level the Black-Scholes model at maturity.
	 * The method call "generateQuantizer()" allows for the generation of the quantization grid.
	 * "generateCompanionWeights()" generates all the companions weights for the quantizer,
	 * thus enabling to create the Voronoi quantization of the Black-Scholes model at maturity.
	 * This discrete variate can be obtained through the method "getVoronoiQuantization", the corresponding quantization error via "getQuantizationError", 
	 * its discrete state space with "getQuantizationGrid" and its law (probabilities) by "getCompanionWeights".
	 * The other constructor are similar to the first one except for the way to use/call/create the Black-Scholes model.
	 * 
	 * @param maturity
	 * @param level
	 * @param model
	 */
	public QuantizableBlackScholesModel(double maturity, int level, BlackScholesModel model) {
		this.maturity = maturity;
		this.level = level;
		this.model = model;
		this.quantizer = new double[level];
		this.companionWeights = new double[level];
		TrapezoidalRealIntegrator mc = new TrapezoidalRealIntegrator(0.01, 100, 50);
		this.f = z -> (1.0 / z*Math.PI)*mc.integrate(u -> (((((Complex.I).negate()).multiply(u*Math.log(z))).exp()).multiply( ((model.apply(maturity)).apply(new Complex(u,0))).multiply( Math.exp(maturity*model.getRiskFreeRate()))) ).getReal()   );
		generateQuantizer();
		generateCompanionWeights();
	}
	
	public QuantizableBlackScholesModel(double maturity, int level, double initialValue, double riskFreeRate, double volatility) {
		this.maturity = maturity;
		this.level = level;
		this.model = new BlackScholesModel(initialValue, riskFreeRate, volatility);
		this.quantizer = new double[level];
		this.companionWeights = new double[level];
		TrapezoidalRealIntegrator mc = new TrapezoidalRealIntegrator(0.01, 100, 50);
		this.f = z -> (1.0 / z*Math.PI)*mc.integrate(u -> (((((Complex.I).negate()).multiply(u*Math.log(z))).exp()).multiply( ((model.apply(maturity)).apply(new Complex(u,0))).multiply( Math.exp(maturity*riskFreeRate))) ).getReal()   );
		generateQuantizer();
		generateCompanionWeights();
	}
	
	public double getInitialValue() {
		return (this.model).getInitialValue();
	}
	
	public double getVolatility() {
		return (this.model).getVolatility();
	}
	
	public double getRiskFreeRate() {
		return (this.model).getRiskFreeRate();
	}
	
	@Override
	public double getMaturity() {
		return this.maturity;
	}
	
	@Override
	public int getLevel() {
		return this.level;
	}
	
	@Override
	public BlackScholesModel getUnderlyingModel() {
		return this.model;
	}

	@Override
	public double getQuantizationError() {
		return getQuantizationError(this.quantizer);
	}

	@Override
	public double[] getQuantizationGrid() {
		return this.quantizer;
	}

	@Override
	public RandomVariableInterface getVoronoiQuantization() {
		return new RandomVariable(this.maturity, this.getQuantizationGrid());
	}

	@Override
	public RandomVariableInterface getCompanionWeights() {
		return new RandomVariable(this.maturity, this.companionWeights);
	}

	@Override
	public Quantizable1DProcessInterface getCloneForModifiedParameters(double[] parameters) {
		return new QuantizableBlackScholesModel(this.maturity, this.level, (this.model).getCloneForModifiedParameters(parameters));
	}
	
	private void generateQuantizer() {
	
		TrapezoidalRealIntegrator mc = new TrapezoidalRealIntegrator(0.01, 100, 50);
		
	    /* Initial guess for the Newton-Raphson algorithm. */
        double e = this.getInitialValue()*Math.exp(this.maturity*this.getRiskFreeRate()); // Expectation of the state variable.
        double[] x0 = new double[this.level];
        double h = 2*Math.sqrt(e) / this.level;
        x0[0] = e - Math.sqrt(e);
        x0[this.level-1] = e + Math.sqrt(e);
        for(int j = 1; j < this.level-1; j++) {
            x0[j] = e - Math.sqrt(e) + j*h;
        }
		double[] v = x0;
		
		/* Start of the Newton-Raphsonalgorithm. */
		int maxIterations = 100;
		double xTolerance = 0.01;
		for(int l = 0; l < maxIterations; l++) {
					
			double[] w = v;			
			double[][] d = new double[this.level][this.level]; // This will stand for the Jacobian matrix of the distortion function.
			double[] g = new double[this.level]; // The gradient of the distortion function
			
			g[0] = (2.0 / Math.PI)*v[0]*mc.integrate(u -> ((((((Complex.I).negate()).multiply(u*Math.log(w[0]))).exp()).multiply( ((model.apply(maturity)).apply(new Complex(u,0))).multiply(Math.exp(maturity*getRiskFreeRate())) )).multiply( ComplexSpecialFunctions.beta(new Complex(0,-u), new Complex (2,0)).subtract(ComplexSpecialFunctions.beta(new Complex(-1,u), new Complex(2,0)).subtract(ComplexSpecialFunctions.incompleteBeta(2*w[0] / (w[0]+w[1]), new Complex(-1,u), new Complex(2,0))  )) )).getReal()   );
			g[this.level-1] = (2.0 / Math.PI)*v[this.level-1]*mc.integrate(u -> ((((((Complex.I).negate()).multiply(u*Math.log(w[level-1]))).exp()).multiply( ((model.apply(maturity)).apply(new Complex(u,0))).multiply( Math.exp(maturity*getRiskFreeRate())))).multiply( (ComplexSpecialFunctions.beta(new Complex(0,-u), new Complex(2,0)).subtract(ComplexSpecialFunctions.incompleteBeta((w[level-2]+w[level-1]) / 2*w[level-1],new Complex(0,-u), new Complex(2,0)))).subtract(ComplexSpecialFunctions.beta(new Complex(-1,u), new Complex(2,0)))     ) ).getReal()   );
		
			d[0][0] = (2.0 / Math.PI)*mc.integrate(u -> ((((((Complex.I).negate()).multiply(u*Math.log(w[0]))).exp()).multiply( ((model.apply(maturity)).apply(new Complex(u,0))).multiply( Math.exp(maturity*getRiskFreeRate())))).multiply( ComplexSpecialFunctions.beta(new Complex(0,-u), new Complex(1,0)).add( ComplexSpecialFunctions.beta(new Complex(0,u), new Complex(1,0)).subtract(ComplexSpecialFunctions.incompleteBeta(2*w[0] / (w[1]+w[0]), new Complex(0,u), new Complex(1,0)))      )) ).getReal()   ) + (-2.0 / ((v[0]+v[1])*Math.PI))*(v[1]-v[0])*0.5*mc.integrate(u -> ((((Complex.I).negate()).multiply(u*Math.log((w[0]+w[1])*0.5)).exp()).multiply( ((model.apply(maturity)).apply(new Complex(u,0))).multiply( Math.exp(maturity*getRiskFreeRate())))).getReal()   );
			d[this.level-1][this.level-1] = (2.0 / Math.PI)*mc.integrate(u -> ((((((Complex.I).negate()).multiply(u*Math.log(w[level-1]))).exp()).multiply( ((model.apply(maturity)).apply(new Complex(u,0))).multiply( Math.exp(maturity*getRiskFreeRate())))).multiply( (ComplexSpecialFunctions.beta(new Complex(0,-u), new Complex(1,0)).subtract(ComplexSpecialFunctions.incompleteBeta((w[level-2]+w[level-1]) / 2*w[level-1], new Complex(0,-u), new Complex(1,0))) ).add(ComplexSpecialFunctions.beta(new Complex(0,u), new Complex(1,0)) )) ).getReal()   ) + (-2.0 / ((v[level-2]+v[level-1])*Math.PI))*(v[level-1]-v[level-2])*0.5*mc.integrate(u -> ((((Complex.I).negate()).multiply(u*Math.log((w[level-2]+w[level-1])*0.5)).exp()).multiply( ((model.apply(maturity)).apply(new Complex(u,0))).multiply( Math.exp(maturity*getRiskFreeRate())))).getReal()   );
			d[0][1] = (-2.0 / ((v[0]+v[1])*Math.PI))*(v[1]-v[0])*0.5*mc.integrate(u -> ((((Complex.I).negate()).multiply(u*Math.log((w[0]+w[1])*0.5)).exp()).multiply( ((model.apply(maturity)).apply(new Complex(u,0))).multiply( Math.exp(maturity*getRiskFreeRate())))).getReal()   );
			d[this.level-1][this.level-2] = (-2.0 / ((v[this.level-2]+v[this.level-1])*Math.PI))*(v[this.level-1]-v[this.level-2])*0.5*mc.integrate(u -> ((((Complex.I).negate()).multiply(u*Math.log((w[level-2]+w[level-1])*0.5)).exp()).multiply( ((model.apply(maturity)).apply(new Complex(u,0))).multiply( Math.exp(maturity*getRiskFreeRate())))).getReal()   );
	
			for(int i = 1; i < this.level-1; i++) {
				
				int j = i;
				g[j] = (2.0 / Math.PI)*v[j]*mc.integrate(u -> ((((((Complex.I).negate()).multiply(u*Math.log(w[j]))).exp()).multiply( ((model.apply(maturity)).apply(new Complex(u,0))).multiply( Math.exp(maturity*getRiskFreeRate())))).multiply( (ComplexSpecialFunctions.beta(new Complex(0,-u), new Complex(2,0)).subtract(ComplexSpecialFunctions.incompleteBeta((w[j]+w[j-1])/2*w[j], new Complex(0,-u), new Complex(2,0)))  ).subtract(ComplexSpecialFunctions.beta(new Complex(-1,u), new Complex(2,0)).subtract(ComplexSpecialFunctions.incompleteBeta(2*w[j] / (w[j]+w[j+1]), new Complex(-1,u), new Complex(2,0))) ) )).getReal()   );
				d[j][j-1] = (-2.0 / ((v[j]+v[j-1])*Math.PI))*(v[j]-v[j-1])*0.5*mc.integrate(u -> ((((Complex.I).negate()).multiply(u*Math.log((w[j-1]+w[j])*0.5)).exp()).multiply( ((model.apply(maturity)).apply(new Complex(u,0))).multiply( Math.exp(maturity*getRiskFreeRate())))).getReal()   );
				d[j][j+1] = (-2.0 / ((v[j+1]+v[j])*Math.PI))*(v[j+1]-v[j])*0.5*mc.integrate(u -> ((((Complex.I).negate()).multiply(u*Math.log((w[j+1]+w[j])*0.5)).exp()).multiply( ((model.apply(maturity)).apply(new Complex(u,0))).multiply( Math.exp(maturity*getRiskFreeRate())))).getReal()   );
				d[j][j] = (2.0 / Math.PI)*mc.integrate(u -> ((((((Complex.I).negate()).multiply(u*Math.log(w[j]))).exp()).multiply( ((model.apply(maturity)).apply(new Complex(u,0))).multiply( Math.exp(maturity*getRiskFreeRate())))).multiply( (ComplexSpecialFunctions.beta(new Complex(0,-u), new Complex(1,0)).subtract(ComplexSpecialFunctions.incompleteBeta((w[j-1]+w[j]) / 2*w[j], new Complex(0,-u), new Complex(1,0))) ).add(ComplexSpecialFunctions.beta(new Complex(0,u), new Complex(1,0)).subtract(ComplexSpecialFunctions.incompleteBeta(2*w[j] / (w[j+1]+w[j]), new Complex(0,u), new Complex(1,0)))      )) ).getReal()  ) + d[j][j+1] + d[j][j-1];	
				
			}
			
			/* Computation of the determinant of the Jacobi tridiagonal matrix d : */
			double[] theta = new double[this.level];
			theta[0] = d[0][0];
			theta[1] = d[0][0]*d[1][1] - d[0][1]*d[1][0];
			for(int n = 2; n < this.level; n++) {
				theta[n] = theta[n-1]*d[n][n] - theta[n-2]*d[n][n-1]*d[n-1][n];	
			}
	
			double[] phi = new double[this.level+2];
			phi[this.level+1] = 0;
			phi[this.level] = 1;
			phi[this.level-1] = d[this.level-1][this.level-1];
			for(int n = this.level-2; n >= 0; n--) {
				phi[n] = phi[n+1]*d[n][n] - phi[n+2]*d[n][n+1]*d[n+1][n];
			}
	
			/* Computation of the inverse matrix of the matrix d : */
			double[][] m = new double[this.level][this.level];
			for(int i = 0; i < this.level; i++) {
				if(i == 0) {
					m[0][0] = phi[1] / theta[this.level-1];
				} else {
					for(int j = 0; j <= i; j++) {
						if(j == i) {
							m[i][j] = theta[i-1]*phi[i+1] / theta[this.level-1];
						} else if(j == 0) {
							double p = 1;
							for(int k = j+1; k < i+1; k++) {
								p = p*d[k][k-1];
							}
							m[i][j] = Math.pow(-1, i+j)*p*(phi[i+1] / theta[this.level-1]);
						} else {
							double x = 1;
							for(int k = j+1; k < i+1; k++) {
								x = x*d[k][k-1];
							}
							m[i][j] = Math.pow(-1, i+j)*x*(theta[j-1]*phi[i+1] / theta[this.level-1]);
						}
					}	 
				}
			}
			
			for(int j = 0; j < this.level ; j++) {
				for(int i = 0; i < j; i++) {
					m[i][j] = m[j][i];
				}
			}
		
			/* Newton-Raphson iteration. */
			double[] r = new double[this.level];
			
			for(int i = 0; i < this.level; i++) {
				
				double sum = 0;
				for(int j = 0; j < this.level; j++) {
					sum = sum + m[i][j]*g[j];
				}
		
				r[i] = Math.abs(v[i]-sum); // We make sure that we get a quantization grid with only positive components after iteration.
				
			}
			
			Arrays.sort(r); // Every grid has to be sorted and contains only positive numbers before using the algorithm.
			
			RealVector previous = new ArrayRealVector(v);
			RealVector current = new ArrayRealVector(r);
			double distance = previous.subtract(current).getNorm();

			v = r;

			if(distance < xTolerance) {
			break;
			}
			
		} 
				
		this.quantizer = v;
		
	}
	
	private void generateCompanionWeights() {
		
		TrapezoidalRealIntegrator mc = new TrapezoidalRealIntegrator(0.01, 100, 50);
		double sum = 0;
		
		this.companionWeights[0] = Math.abs(0.5-((1.0 / Math.PI)*mc.integrate(u -> (((((Complex.I).negate()).multiply(u*Math.log((quantizer[0]+quantizer[1])*0.5)).exp()).multiply( ((model.apply(maturity)).apply(new Complex(u,0))).multiply( Math.exp(maturity*getRiskFreeRate())))).divide(new Complex(0,u))).getReal()   )));
		this.companionWeights[this.level-1] = Math.abs(0.5+((1.0 / Math.PI)*mc.integrate(u -> (((((Complex.I).negate()).multiply(u*Math.log((quantizer[level-1]+quantizer[level-2])*0.5)).exp()).multiply( ((model.apply(maturity)).apply(new Complex(u,0))).multiply( Math.exp(maturity*getRiskFreeRate())))).divide(new Complex(0,u))).getReal()  )));
		sum = this.companionWeights[0];
		
		/* Use of the Cumulative distribution function of the variable to quantize in order to compute the companion weights. */
		for(int j = 1; j < this.level-1; j++) {
			
			int i = j;
			this.companionWeights[i] = Math.abs((mc.integrate(u -> (((((Complex.I).negate()).multiply(u*Math.log((quantizer[i]+quantizer[i-1])*0.5)).exp()).multiply( ((model.apply(maturity)).apply(new Complex(u,0))).multiply( Math.exp(maturity*getRiskFreeRate())))).divide(new Complex(0,u))).getReal() ) - mc.integrate(u -> (((((Complex.I).negate()).multiply(u*Math.log((this.quantizer[i]+this.quantizer[i+1])*0.5)).exp()).multiply( ((model.apply(maturity)).apply(new Complex(u,0))).multiply( Math.exp(maturity*getRiskFreeRate())))).divide(new Complex(0,u))).getReal()   )) / Math.PI);
			sum = sum + this.companionWeights[i];
			
		}
		
		sum = sum + this.companionWeights[this.level-1];
	
		for(int i = 0; i < level; i++) {
			this.companionWeights[i] = this.companionWeights[i] / sum;
		}

	}
	
	private double getQuantizationError(double[] grid) {
		
		double[] d = new double[this.level]; // Computation of the probability density function of the underlying process evaluated at maturity.
		// This one is required to compute the summands needed for the computation of the quantization error.
		TrapezoidalRealIntegrator mc2 = new TrapezoidalRealIntegrator(0.01, (grid[0]+grid[1])*0.5, 50);
		TrapezoidalRealIntegrator mc3 = new TrapezoidalRealIntegrator((grid[this.level-1]+grid[this.level-2])*0.5, 100, 50);
		
		d[0] = Math.abs(mc2.integrate(z -> f.applyAsDouble(z)*Math.pow(z-grid[0],2)));
		d[this.level-1] = Math.abs(mc3.integrate(z -> f.applyAsDouble(z)*Math.pow(z-grid[level-1],2)));
		
		for(int i = 1; i < this.level-1; i++) {
			
			int j = i;
			TrapezoidalRealIntegrator mc = new TrapezoidalRealIntegrator((grid[i]+grid[i-1])*0.5, (grid[i]+grid[i+1])*0.5, 50);
			d[i] = Math.abs(mc.integrate(z -> f.applyAsDouble(z)*Math.pow(z-grid[j],2)));
			
		}
		
		return Math.sqrt(DoubleStream.of(d).sum());
		
	}
	
}
