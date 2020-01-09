package net.finmath.functions;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.util.CombinatoricsUtils;

import java.lang.Math;

/*
 * Class consisting of static functions extending the special Gamma and Beta functions along with their derivatives, 
 * to the whole complex plane almost everywhere. 
 */
public class ComplexSpecialFunctions {
	
	/*
	 * Implementation of the Lanczos approximation of the complex gamma function.
	 */
	public static Complex gamma(Complex x) {

		double[] p = {0.99999999999980993, 676.5203681218851, -1259.1392167224028,
				     	  771.32342877765313, -176.61502916214059, 12.507343278686905,
				     	  -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7};
		int g = 7;
			
		if(x.getReal() < 0.5) {
				
			Complex complexPi = new Complex(Math.PI,0);
			return complexPi.divide(((x.multiply(Math.PI)).sin()).multiply(gamma((x.negate()).add(1.0))));
				
		} else {
	 
			x = x.subtract(1.0);
			
			Complex a = new Complex(p[0],0);
			Complex t = x.add(g+0.5);
		

			for(int i = 1; i < p.length; i++) {
				Complex pi = new Complex(p[i],0);		
				a = a.add(pi.divide(x.add((double)i)));
			}
	
			return (((t.pow(x.add(0.5))).multiply((t.negate()).exp())).multiply(a)).multiply(Math.sqrt(2*Math.PI));
		}
	}
	
	/*
	 * Extension of the beta function to complex parameters.
	 */
	public static Complex beta(Complex x, Complex y) {
		return (gamma(x).multiply(gamma(y))).divide(gamma(x.add(y)));
	}
	
	/*
	 * Extension of the incomplete beta function to complex inputs by means of the hypergeometric function.
	 */
	public static Complex incompleteBeta(double x, Complex u, Complex v) {
		Complex sum = new Complex(0);
		for(int i = 0; i < 10; i++) {
			sum = sum.add(
					(gamma(u.add(i)).multiply(gamma(v.negate().add(1+i))).multiply(Math.pow(x, i))).divide(gamma(u.add(1+i)).multiply(CombinatoricsUtils.factorialDouble(i)))     );
		}
		return ((new Complex(x,0).pow(u)).divide(u))
				.multiply(
						sum.multiply(
								gamma(u.add(1)).divide(
										gamma(u).multiply(gamma(v.negate().add(1))))));
	}
	
	
}
