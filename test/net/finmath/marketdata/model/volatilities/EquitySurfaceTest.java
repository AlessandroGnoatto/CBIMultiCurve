package net.finmath.marketdata.model.volatilities;

import java.time.LocalDate;

import net.finmath.marketdata.model.curves.Curve.ExtrapolationMethod;
import net.finmath.marketdata.model.curves.Curve.InterpolationEntity;
import net.finmath.marketdata.model.curves.Curve.InterpolationMethod;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface.QuotingConvention;
import net.finmath.marketdata.model.volatilities.OptionSurfaceData;

public class EquitySurfaceTest {
	
	public static void main(String[] args){
		
		String underlying = "GOOG";
		double[] strikes = {90,100};
		double[] maturities = {1,2};
		double[][] values = {{0.25,0.23},{0.3,0.28}};
		LocalDate referenceDate = LocalDate.of(2017, 03, 29);
		
		QuotingConvention convention = QuotingConvention.VOLATILITYLOGNORMAL;
		QuotingConvention priceConvention = QuotingConvention.PRICE;
		
		double r = 0.04;
		
		ExtrapolationMethod exMethod = ExtrapolationMethod.CONSTANT;
		InterpolationMethod intMethod = InterpolationMethod.LINEAR;
		InterpolationEntity intEntity = InterpolationEntity.LOG_OF_VALUE;
		
		DiscountCurveInterface myCurve1 = DiscountCurve.createDiscountCurveFromDiscountFactors(
				"discountCurve"								/* name */,
				new double[] {0.0,  1.0,  2.0,  4.0,  5.0}	/* maturities */,
				new double[] {1.0, Math.exp(-r*1.0), Math.exp(-r*2.0), Math.exp(-r*3.0), Math.exp(-r*4.0)}	/* discount factors */,
				intMethod,exMethod,intEntity
				);
		
		
		
		DiscountCurveInterface myCurve2 = DiscountCurve.createDiscountCurveFromDiscountFactors(
				"discountCurve"								/* name */,
				new double[] {0.0,  1.0,  2.0,  4.0,  5.0}	/* maturities */,
				new double[] {100, 100*Math.exp(r*1.0), 100*Math.exp(r*2.0), 100*Math.exp(r*3.0), 100*Math.exp(r*4.0)}	/* discount factors */,
				intMethod,exMethod,intEntity
				);
		
		OptionSurfaceData myGoogleSurface = new OptionSurfaceData(underlying, referenceDate, strikes, maturities, values, convention, myCurve1, myCurve2);
		
		double requestedStrike = 90;
		double requestedMaturity = 1;
		
		double myQuote = myGoogleSurface.getValue(requestedMaturity, requestedStrike);
		double equivalentPrice = myGoogleSurface.getValue(requestedMaturity, requestedStrike,priceConvention);
		
		System.out.println("Warning: no interpolation of market data is performed here. Only available points are returned.");
		//Here we should recover 0.25
		System.out.println("We ask a surface to return the values: " + myQuote);
		//The expected price for S0 = 100 r=0.04 q=0 K=90 T=1 is 17.55
		System.out.println("We can ask the surface to convert vols into prices: " +equivalentPrice);
		
	}

}
