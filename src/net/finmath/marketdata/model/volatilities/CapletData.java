package net.finmath.marketdata.model.volatilities;

import java.time.LocalDate;

import net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface.QuotingConvention;

public class CapletData {
	private final String underlyingCurve;
	private final String discountCurve;
	private final LocalDate referenceDate;
	private final double strike;
	private final double maturity;
	private final double value;
	private final QuotingConvention convention;
	
	public CapletData(String underlyingCurve, String discountCurve, LocalDate referenceDate, double strike,
			double maturity, double value, QuotingConvention convention) {
		super();
		this.underlyingCurve = underlyingCurve;
		this.discountCurve = discountCurve;
		this.referenceDate = referenceDate;
		this.strike = strike;
		this.maturity = maturity;
		this.value = value;
		this.convention = convention;
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

	public double getStrike() {
		return strike;
	}

	public double getMaturity() {
		return maturity;
	}

	public double getValue() {
		return value;
	}

	public QuotingConvention getConvention() {
		return convention;
	}

	@Override
	public String toString() {
		return "CapletData [underlyingCurve=" + underlyingCurve + ", discountCurve=" + discountCurve
				+ ", referenceDate=" + referenceDate + ", strike=" + strike + ", maturity=" + maturity + ", value="
				+ value + ", convention=" + convention + "]";
	}

}
