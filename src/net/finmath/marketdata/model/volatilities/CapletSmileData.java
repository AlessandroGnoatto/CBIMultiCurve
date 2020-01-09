package net.finmath.marketdata.model.volatilities;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;

import net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface.QuotingConvention;

public class CapletSmileData {
	private final String underlyingCurve;
	private final String discountCurve;
	private final LocalDate referenceDate;
	private final double[] strikes;
	private final double maturity;
	private final HashMap<Double,CapletData> smile;
	
	public CapletSmileData(String underlyingCurve, String discountCurve, LocalDate referenceDate, double[] strikes, double maturity, double[] values, QuotingConvention convention){
		if(strikes.length != values.length) {
			throw new IllegalArgumentException("Number of strikes and market quotes does not coincide");
		}else {
			int numberOfQuotes = strikes.length;
			smile = new HashMap<Double,CapletData>();
			for(int i = 0; i< numberOfQuotes; i++) {
				smile.put(strikes[i],new CapletData(underlyingCurve, discountCurve, referenceDate, strikes[i],maturity,values[i], convention)) ;
			}
			this.underlyingCurve = underlyingCurve;
			this.discountCurve = discountCurve;
			this.referenceDate = referenceDate;
			this.strikes = strikes;
			this.maturity = maturity;
		}
	}

	public String getUnderlyingCurve() {
		return underlyingCurve;
	}

	public String getDiscountCurve() {
		return discountCurve;
	}

	public LocalDate getReferenceDate() {
		return referenceDate;
	}

	public double[] getStrikes() {
		return strikes;
	}

	public double getMaturity() {
		return maturity;
	}

	public HashMap<Double, CapletData> getSmile() {
		return smile;
	}
	
	public CapletData getOption(double strike) {
		return smile.get(strike);
	}

	@Override
	public String toString() {
		return "CapletSmileData [underlyingCurve=" + underlyingCurve + ", discountCurve=" + discountCurve
				+ ", referenceDate=" + referenceDate + ", strikes=" + Arrays.toString(strikes) + ", maturity="
				+ maturity + ", smile=" + smile + "]";
	}

}
