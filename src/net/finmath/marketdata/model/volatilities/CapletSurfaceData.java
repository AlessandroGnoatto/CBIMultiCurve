package net.finmath.marketdata.model.volatilities;

import java.time.LocalDate;
import java.util.HashMap;

import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface.QuotingConvention;

public class CapletSurfaceData {
	
	private final LocalDate referenceDate;
	private final AnalyticModelInterface curves;
	private final QuotingConvention convention;
	private final HashMap<Double, CapletSmileData> surface;
	private final double[] maturities;
	
	/**
	 * Creates an equity option surface from an array of smiles.
	 * @param smiles
	 * @param discountCurve
	 * @param equityForwardCurve
	 */
	public CapletSurfaceData(CapletSmileData[] smiles, AnalyticModelInterface curves) {
		
		CapletSmileData firstSmile = smiles[0];
		LocalDate myReferenceDate = firstSmile.getReferenceDate();
		QuotingConvention myConvention = firstSmile.getSmile().get(firstSmile.getStrikes()[0]).getConvention();
		
		HashMap<Double, CapletSmileData> mySurface = new HashMap<Double, CapletSmileData>();
		double[] mats = new double[smiles.length];
		
		for(int t = 0; t<smiles.length;t++) {
			double maturity = smiles[t].getMaturity();
			mats[t] = maturity;
			
			if(!(smiles[t].getReferenceDate().equals(myReferenceDate)))
				throw new IllegalArgumentException("All reference dates must be equal");
						
			QuotingConvention testConvention = smiles[t].getSmile().get(smiles[t].getStrikes()[0]).getConvention();
			
			if(!(testConvention.equals(myConvention)))
				throw new IllegalArgumentException("Convention must be the same for all points in the surface");
			
			mySurface.put(maturity, smiles[t]);
		}
		
		this.referenceDate = myReferenceDate;
		this.curves = curves;
		this.convention = myConvention;
		this.surface = mySurface;
		this.maturities = mats;
	
	}

	public LocalDate getReferenceDate() {
		return referenceDate;
	}

	public AnalyticModelInterface getCurves() {
		return curves;
	}

	public QuotingConvention getConvention() {
		return convention;
	}

	public HashMap<Double, CapletSmileData> getSurface() {
		return surface;
	}

	public double[] getMaturities() {
		return maturities;
	}	
	
	public double getValue(double maturity, double strike, QuotingConvention quotingConvention) {
		CapletSmileData relevantSmile = this.surface.get(maturity);
		
		String discountCurve = relevantSmile.getDiscountCurve();
		String forwardCurve = relevantSmile.getUnderlyingCurve();		
		//When you ask finmath for a forward,you must always input T - \delta
		double delta = underlyingToTenor(forwardCurve);
		
		if(quotingConvention.equals(this.convention)) {
			return relevantSmile.getSmile().get(strike).getValue();
		}else {
			if(quotingConvention == QuotingConvention.PRICE && this.convention == QuotingConvention.VOLATILITYNORMAL) {
				
				double forwardPrice = this.curves.getForwardCurve(forwardCurve).getValue(maturity - delta);
				double discountBond = this.curves.getDiscountCurve(discountCurve).getDiscountFactor(maturity);
				double volatility = relevantSmile.getSmile().get(strike).getValue();
				return net.finmath.functions.AnalyticFormulas.bachelierOptionValue(forwardPrice, volatility, maturity, strike, delta * discountBond);
				
			}else if(quotingConvention == QuotingConvention.VOLATILITYNORMAL && this.convention == QuotingConvention.PRICE) {
				
				double forwardPrice = this.curves.getForwardCurve(forwardCurve).getValue(maturity - delta);
				double discountBond = this.curves.getDiscountCurve(discountCurve).getDiscountFactor(maturity);
				double price = relevantSmile.getSmile().get(strike).getValue();
				
				return net.finmath.functions.AnalyticFormulas.bachelierOptionImpliedVolatility(forwardPrice,maturity,strike,delta * discountBond,price);
			
			}else if(quotingConvention == QuotingConvention.PRICE && this.convention == QuotingConvention.VOLATILITYLOGNORMAL){
				
				
				double forwardPrice = this.curves.getForwardCurve(forwardCurve).getValue(maturity - delta);
				double discountBond = this.curves.getDiscountCurve(discountCurve).getDiscountFactor(maturity);
				double volatility = relevantSmile.getSmile().get(strike).getValue();
				
				return net.finmath.functions.AnalyticFormulas.blackScholesGeneralizedOptionValue(forwardPrice, volatility, maturity, strike, delta *discountBond);
				
			}else if(quotingConvention == QuotingConvention.VOLATILITYLOGNORMAL && this.convention == QuotingConvention.PRICE) {
				
				double forwardPrice = this.curves.getForwardCurve(forwardCurve).getValue(maturity - delta);
				double discountBond = this.curves.getDiscountCurve(discountCurve).getDiscountFactor(maturity);
				double price = relevantSmile.getSmile().get(strike).getValue();
				return net.finmath.functions.AnalyticFormulas.blackScholesOptionImpliedVolatility(forwardPrice,maturity,strike,delta * discountBond,price);
			}
				
			return 0.0;
		}		
		
	}
	
	public CapletSmileData getSmile(double maturity) {
		return surface.get(maturity);
	}
		
	private double underlyingToTenor(String underlyingName) {
		if(underlyingName.contains("3M")) {
			return 0.25;
		}else if(underlyingName.contains("6M")) {
			return 0.5;
		}else if(underlyingName.contains("12M")) {
			return 1.0;
			
		}else if(underlyingName.contains("1Y")) {
			return 1.0;
			
		}else {
			throw new IllegalArgumentException("Tenor not recognized.");
		}
	}

}
