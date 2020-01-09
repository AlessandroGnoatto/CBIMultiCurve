package net.finmath.fouriermethod.quantization;
 
import net.finmath.fouriermethod.calibration.models.BlackScholesModel;
import net.finmath.fouriermethod.quantization.QuantizableBlackScholesModel;
import net.finmath.fouriermethod.quantization.QuantizationBlackScholesOptionPricer;
import net.finmath.functions.AnalyticFormulas;
 
public class TestBlackScholesQuantization {
 
    public static void main(String[] args) {
         
        double initialValue = 100.0;
        double volatility = 1.5;
        double riskFreeRate = 0.04;
 
        double maturity = 10.0;
         
        int level = 20;
         
        //Create Model (Characteristic function)
        BlackScholesModel bsModel = new BlackScholesModel(initialValue, riskFreeRate, volatility);
         
        //Quantize the model.
        long startMillis = System.currentTimeMillis();
        QuantizableBlackScholesModel quantizedModel = new QuantizableBlackScholesModel(maturity, level, bsModel);
        long endMillis = System.currentTimeMillis();
        System.out.println("Time required for finding the quantization grid of level " + level + ": " + ((endMillis-startMillis)/1000.0) + " sec.");
         
         
        System.out.println("Black-Scholes call prices for a maturity of " + maturity + ", a risk-free rate of " + riskFreeRate + ", an initial value of " + initialValue + " and a volatility of " + volatility + ", against Quantization:");
         
        double[] strikes = new double[20];
         
        for(int i = 0; i< 20;i++) {
            strikes[i] = 10+10.0*i;
             
            QuantizationBlackScholesOptionPricer product = new QuantizationBlackScholesOptionPricer(strikes[i], maturity);
 
            double value = product.getValue(quantizedModel);
             
            double valueAnalytic = AnalyticFormulas.blackScholesOptionValue(initialValue, riskFreeRate, volatility, maturity, strikes[i]);
 
            double error = Math.abs(value - valueAnalytic);
 
            System.out.println("Strike: " + strikes[i] + ", B-S: " + valueAnalytic + ", " + "Quantization: " + value + ", " + "Error: " + error + ".");
 
        }
    }
 
}