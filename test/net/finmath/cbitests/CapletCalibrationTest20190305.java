package net.finmath.cbitests;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import net.finmath.fouriermethod.calibration.CapletCalibrationProblem;
import net.finmath.fouriermethod.calibration.models.CBIDrivenMultiCurveModel;
import net.finmath.fouriermethod.products.CapletByCarrMadan;
import net.finmath.marketdata.calibration.CalibratedCurves;
import net.finmath.marketdata.calibration.CalibratedCurves.CalibrationSpec;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.Curve;
import net.finmath.marketdata.model.curves.CurveInterface;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveFromDiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.optimizer.OptimizerFactoryInterface;
import net.finmath.optimizer.OptimizerFactoryLevenbergMarquardt;
import net.finmath.optimizer.SolverException;
import net.finmath.stochastic.MultiCurveTenor;
import net.finmath.marketdata.model.curves.Curve.ExtrapolationMethod;
import net.finmath.marketdata.model.curves.Curve.InterpolationEntity;
import net.finmath.marketdata.model.curves.Curve.InterpolationMethod;
import net.finmath.marketdata.model.volatilities.CapletSmileData;
import net.finmath.marketdata.model.volatilities.CapletSurfaceData;
import net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface.QuotingConvention;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.ScheduleInterface;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface;

public class CapletCalibrationTest20190305 {

	public static void main(String[] args) throws SolverException, CloneNotSupportedException {
		
		/*
		 * Calibration of a single curve - OIS curve - self disocunted curve, from a set of calibration products.
		 */
		LocalDate referenceDate = LocalDate.of(2018,9,24);

		/*
		 * Define the calibration spec generators for our calibration products
		 */
		Function<String,String> frequencyForTenor = (tenor) -> {
			switch(tenor) {
			case "3M":
				return "quarterly";
			case "6M":
				return "semiannual";
			}
			throw new IllegalArgumentException("Unkown tenor " + tenor);
		};

		BiFunction<String, Double, CalibrationSpec> deposit = (maturity, rate) -> {
			ScheduleInterface scheduleInterfaceRec = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, "0D", maturity, "tenor", "act/360", "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), 0, 0);
			ScheduleInterface scheduleInterfacePay = null;
			double calibrationTime = scheduleInterfaceRec.getPayment(scheduleInterfaceRec.getNumberOfPeriods()-1);
			CalibrationSpec calibrationSpec = new CalibratedCurves.CalibrationSpec("EUR-OIS-" + maturity, "Deposit", scheduleInterfaceRec, "", rate, "discount-EUR-OIS", scheduleInterfacePay, null, 0.0, null, "discount-EUR-OIS", calibrationTime);
			return calibrationSpec;
		};

		BiFunction<String, Double, CalibrationSpec> swapSingleCurve = (maturity, rate) -> {
			ScheduleInterface scheduleInterfaceRec = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, "0D", maturity, "annual", "act/360", "first", "modified_following", new BusinessdayCalendarExcludingTARGETHolidays(), 0, 1);
			ScheduleInterface scheduleInterfacePay = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, "0D", maturity, "annual", "act/360", "first", "modified_following", new BusinessdayCalendarExcludingTARGETHolidays(), 0, 1);
			double calibrationTime = scheduleInterfaceRec.getPayment(scheduleInterfaceRec.getNumberOfPeriods() - 1);
			CalibrationSpec calibrationSpec = new CalibratedCurves.CalibrationSpec("EUR-OIS-" + maturity, "Swap", scheduleInterfaceRec, "forward-EUR-OIS", 0.0, "discount-EUR-OIS", scheduleInterfacePay, "", rate, "discount-EUR-OIS", "discount-EUR-OIS", calibrationTime);
			return calibrationSpec;
		};

		Function<String,BiFunction<String, Double, CalibrationSpec>> fra = (tenor) -> {
			return (fixing, rate) -> {
				ScheduleInterface scheduleInterfaceRec = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, fixing, tenor, "tenor", "act/360", "first", "modified_following", new BusinessdayCalendarExcludingTARGETHolidays(), 0, 0);
				double calibrationTime = scheduleInterfaceRec.getFixing(scheduleInterfaceRec.getNumberOfPeriods() - 1);
				String curveName = "forward-EUR-" + tenor;
				CalibrationSpec calibrationSpec = new CalibratedCurves.CalibrationSpec("EUR-" + tenor + "-" + fixing, "FRA", scheduleInterfaceRec, curveName, rate, "discount-EUR-OIS", null, null, 0.0, null, curveName, calibrationTime);
				return calibrationSpec;
			};
		};

		Function<String,BiFunction<String, Double, CalibrationSpec>> swap = (tenor) -> {
			return (maturity, rate) -> {
				String frequencyRec = frequencyForTenor.apply(tenor);

				ScheduleInterface scheduleInterfaceRec = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, "0D", maturity, frequencyRec, "act/360", "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), 0, 0);
				ScheduleInterface scheduleInterfacePay = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, "0D", maturity, "annual", "E30/360", "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), 0, 0);
				double calibrationTime = scheduleInterfaceRec.getFixing(scheduleInterfaceRec.getNumberOfPeriods() - 1);
				String curveName = "forward-EUR-" + tenor;
				CalibrationSpec calibrationSpec = new CalibratedCurves.CalibrationSpec("EUR-" + tenor + maturity, "Swap", scheduleInterfaceRec, curveName, 0.0, "discount-EUR-OIS", scheduleInterfacePay, "", rate, "discount-EUR-OIS", curveName, calibrationTime);
				return calibrationSpec;
			};
		};

		BiFunction<String,String,BiFunction<String, Double, CalibrationSpec>> swapBasis = (tenorRec,tenorPay) -> {
			return (maturity, rate) -> {
				String curveNameRec = "forward-EUR-" + tenorRec;
				String curveNamePay = "forward-EUR-" + tenorPay;

				String frequencyRec = frequencyForTenor.apply(tenorRec);
				String frequencyPay = frequencyForTenor.apply(tenorPay);

				ScheduleInterface scheduleInterfaceRec = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, "0D", maturity, frequencyRec, "act/360", "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), 0, 0);
				ScheduleInterface scheduleInterfacePay = ScheduleGenerator.createScheduleFromConventions(referenceDate, 2, "0D", maturity, frequencyPay, "act/360", "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), 0, 0);
				double calibrationTime = scheduleInterfaceRec.getFixing(scheduleInterfaceRec.getNumberOfPeriods() - 1);

				CalibrationSpec calibrationSpec = new CalibratedCurves.CalibrationSpec("EUR-" + tenorRec + "-" + tenorPay + maturity, "Swap", scheduleInterfaceRec, curveNameRec, 0.0, "discount-EUR-OIS", scheduleInterfacePay, curveNamePay, rate, "discount-EUR-OIS", curveNameRec, calibrationTime);
				return calibrationSpec;
			};
		};

		/*
		 * Generate empty curve template (for cloning during calibration)
		 */
		double[] times = { 0.0 };
		double[] discountFactors = { 1.0 };
		boolean[] isParameter = { false };

		DiscountCurve discountCurveOIS = DiscountCurve.createDiscountCurveFromDiscountFactors("discount-EUR-OIS", referenceDate, times, discountFactors, isParameter, InterpolationMethod.LINEAR, ExtrapolationMethod.CONSTANT, InterpolationEntity.LOG_OF_VALUE);
		ForwardCurveInterface forwardCurveOIS = new ForwardCurveFromDiscountCurve("forward-EUR-OIS", "discount-EUR-OIS", referenceDate, "3M");
		ForwardCurveInterface forwardCurve3M = new ForwardCurve("forward-EUR-3M", referenceDate, "3M", new BusinessdayCalendarExcludingTARGETHolidays(), BusinessdayCalendarInterface.DateRollConvention.FOLLOWING, Curve.InterpolationMethod.LINEAR, Curve.ExtrapolationMethod.CONSTANT, Curve.InterpolationEntity.VALUE,ForwardCurve.InterpolationEntityForward.FORWARD, "discount-EUR-OIS");
		ForwardCurveInterface forwardCurve6M = new ForwardCurve("forward-EUR-6M", referenceDate, "6M", new BusinessdayCalendarExcludingTARGETHolidays(), BusinessdayCalendarInterface.DateRollConvention.FOLLOWING, Curve.InterpolationMethod.LINEAR, Curve.ExtrapolationMethod.CONSTANT, Curve.InterpolationEntity.VALUE,ForwardCurve.InterpolationEntityForward.FORWARD, "discount-EUR-OIS");

		AnalyticModel forwardCurveModel = new AnalyticModel(new CurveInterface[] { discountCurveOIS, forwardCurveOIS, forwardCurve3M, forwardCurve6M });

		List<CalibrationSpec> calibrationSpecs = new LinkedList<>();

		/*
		 * Calibration products for OIS curve: Deposits
		 */
		calibrationSpecs.add(deposit.apply("1D", 0.202 / 100.0));
		calibrationSpecs.add(deposit.apply("1W", 0.195 / 100.0));
		calibrationSpecs.add(deposit.apply("2W", 0.193 / 100.0));
		calibrationSpecs.add(deposit.apply("3W", 0.193 / 100.0));
		calibrationSpecs.add(deposit.apply("1M", 0.191 / 100.0));
		calibrationSpecs.add(deposit.apply("2M", 0.185 / 100.0));
		calibrationSpecs.add(deposit.apply("3M", 0.180 / 100.0));
		calibrationSpecs.add(deposit.apply("4M", 0.170 / 100.0));
		calibrationSpecs.add(deposit.apply("5M", 0.162 / 100.0));
		calibrationSpecs.add(deposit.apply("6M", 0.156 / 100.0));
		calibrationSpecs.add(deposit.apply("7M", 0.150 / 100.0));
		calibrationSpecs.add(deposit.apply("8M", 0.145 / 100.0));
		calibrationSpecs.add(deposit.apply("9M", 0.141 / 100.0));
		calibrationSpecs.add(deposit.apply("10M", 0.136 / 100.0));
		calibrationSpecs.add(deposit.apply("11M", 0.133 / 100.0));
		calibrationSpecs.add(deposit.apply("12M", 0.129 / 100.0));

		/*
		 * Calibration products for OIS curve: Swaps
		 */
		calibrationSpecs.add(swapSingleCurve.apply("15M", 0.118 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("18M", 0.108 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("21M", 0.101 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("2Y", 0.101 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("3Y", 0.194 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("4Y", 0.346 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("5Y", 0.534 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("6Y", 0.723 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("7Y", 0.895 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("8Y", 1.054 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("9Y", 1.189 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("10Y", 1.310 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("11Y", 1.423 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("12Y", 1.520 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("15Y", 1.723 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("20Y", 1.826 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("25Y", 1.877 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("30Y", 1.910 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("40Y", 2.025 / 100.0));
		calibrationSpecs.add(swapSingleCurve.apply("50Y", 2.101 / 100.0));

		/*
		 * Calibration products for 3M curve: FRAs
		 */
		calibrationSpecs.add(fra.apply("3M").apply("0D", 0.322 / 100.0));
		calibrationSpecs.add(fra.apply("3M").apply("1M", 0.329 / 100.0));
		calibrationSpecs.add(fra.apply("3M").apply("2M", 0.328 / 100.0));
		calibrationSpecs.add(fra.apply("3M").apply("3M", 0.326 / 100.0));
		calibrationSpecs.add(fra.apply("3M").apply("6M", 0.323 / 100.0));
		calibrationSpecs.add(fra.apply("3M").apply("9M", 0.316 / 100.0));
		calibrationSpecs.add(fra.apply("3M").apply("12M", 0.360 / 100.0));
		calibrationSpecs.add(fra.apply("3M").apply("15M", 0.390 / 100.0));

		/*
		 * Calibration products for 3M curve: swaps
		 */
		calibrationSpecs.add(swap.apply("3M").apply("2Y", 0.380 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("3Y", 0.485 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("4Y", 0.628 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("5Y", 0.812 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("6Y", 0.998 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("7Y", 1.168 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("8Y", 1.316 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("9Y", 1.442 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("10Y", 1.557 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("12Y", 1.752 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("15Y", 1.942 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("20Y", 2.029 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("25Y", 2.045 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("30Y", 2.097 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("40Y", 2.208 / 100.0));
		calibrationSpecs.add(swap.apply("3M").apply("50Y", 2.286 / 100.0));

		/*
		 * Calibration products for 6M curve: FRAs
		 */

		calibrationSpecs.add(fra.apply("6M").apply("0D", 0.590 / 100.0));
		calibrationSpecs.add(fra.apply("6M").apply("1M", 0.597 / 100.0));
		calibrationSpecs.add(fra.apply("6M").apply("2M", 0.596 / 100.0));
		calibrationSpecs.add(fra.apply("6M").apply("3M", 0.594 / 100.0));
		calibrationSpecs.add(fra.apply("6M").apply("6M", 0.591 / 100.0));
		calibrationSpecs.add(fra.apply("6M").apply("9M", 0.584 / 100.0));
		calibrationSpecs.add(fra.apply("6M").apply("12M", 0.584 / 100.0));

		/*
		 * Calibration products for 6M curve: tenor basis swaps
		 * Note: the fixed bases is added to the second argument tenor (here 3M).
		 */
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("2Y", 0.255 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("3Y", 0.245 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("4Y", 0.227 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("5Y", 0.210 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("6Y", 0.199 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("7Y", 0.189 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("8Y", 0.177 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("9Y", 0.170 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("10Y", 0.164 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("12Y", 0.156 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("15Y", 0.135 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("20Y", 0.125 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("25Y", 0.117 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("30Y", 0.107 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("40Y", 0.095 / 100.0));
		calibrationSpecs.add(swapBasis.apply("6M","3M").apply("50Y", 0.088 / 100.0));

		/*
		 * Calibrate
		 */
		CalibratedCurves calibratedCurves = new CalibratedCurves(calibrationSpecs.toArray(new CalibrationSpec[calibrationSpecs.size()]), forwardCurveModel, 1E-15);

		/*
		 * Get the calibrated model
		 */
		AnalyticModelInterface calibratedModel = calibratedCurves.getModel();
		
		double[] strikes = {-0.005,
				-0.0025,
				-0.0013,
				0.0,
				0.0025,
				0.005,
				0.01,
				0.015,
				0.02,
				0.03,
				0.05};
		
	
		double[][] volatilities = 
			{{0.0025,0.0024,0.0027,0.0031,0.0037,0.0043,0.0052,0.0061,0.0069,0.0085,0.0114},
					{0.0025,0.0024,0.0027,0.0031,0.0037,0.0043,0.0052,0.0061,0.0069,0.0085,0.0114},
					{0.0025,0.0024,0.0028,0.0031,0.0038,0.0043,0.0052,0.0061,0.0069,0.0085,0.0114},
					{0.0025,0.0024,0.0027,0.0031,0.0037,0.0043,0.0052,0.0061,0.0069,0.0085,0.0114},
					{0.0025,0.0024,0.0027,0.0031,0.0038,0.0043,0.0052,0.0061,0.0069,0.0085,0.0114},
					{0.0040,0.0043,0.0045,0.0047,0.0049,0.0052,0.0058,0.0065,0.0072,0.0087,0.0114},
					{0.0047,0.0049,0.0051,0.0053,0.0055,0.0057,0.0062,0.0068,0.0075,0.0089,0.0115},
					{0.0050,0.0052,0.0053,0.0054,0.0056,0.0058,0.0063,0.0069,0.0075,0.0088,0.0113},
					{0.0055,0.0056,0.0057,0.0058,0.0060,0.0061,0.0065,0.0070,0.0076,0.0088,0.0113},
					{0.0055,0.0056,0.0057,0.0058,0.0060,0.0061,0.0064,0.0069,0.0074,0.0084,0.0107},
					{0.0058,0.0059,0.0061,0.0061,0.0062,0.0064,0.0066,0.0070,0.0074,0.0083,0.0103},
					{0.0058,0.0059,0.0059,0.0060,0.0062,0.0063,0.0066,0.0069,0.0073,0.0081,0.0099},
					{0.0060,0.0061,0.0062,0.0063,0.0064,0.0065,0.0067,0.0069,0.0072,0.0079,0.0095},
					{0.0059,0.0060,0.0061,0.0061,0.0063,0.0063,0.0066,0.0068,0.0071,0.0079,0.0095},
					{0.0061,0.0062,0.0062,0.0062,0.0064,0.0065,0.0066,0.0068,0.0071,0.0077,0.0092},
					{0.0059,0.0060,0.0061,0.0061,0.0062,0.0063,0.0065,0.0067,0.0070,0.0077,0.0091},
					{0.0060,0.0061,0.0062,0.0062,0.0063,0.0063,0.0066,0.0067,0.0070,0.0075,0.0089},
					{0.0060,0.0060,0.0060,0.0061,0.0062,0.0063,0.0064,0.0067,0.0069,0.0075,0.0088},
					{0.0060,0.0061,0.0061,0.0062,0.0062,0.0063,0.0064,0.0067,0.0068,0.0073,0.0086},
					{0.0058,0.0058,0.0059,0.0059,0.0060,0.0061,0.0063,0.0065,0.0067,0.0073,0.0085},
					{0.0058,0.0059,0.0060,0.0060,0.0060,0.0061,0.0063,0.0065,0.0067,0.0071,0.0083},
					{0.0059,0.0059,0.0060,0.0060,0.0061,0.0061,0.0063,0.0064,0.0066,0.0070,0.0081},
					{0.0059,0.0060,0.0061,0.0061,0.0061,0.0062,0.0063,0.0064,0.0065,0.0069,0.0079},
					{0.0056,0.0057,0.0057,0.0057,0.0058,0.0059,0.0060,0.0062,0.0065,0.0069,0.0081},
					{0.0056,0.0057,0.0057,0.0057,0.0058,0.0059,0.0060,0.0062,0.0064,0.0068,0.0079},
					{0.0057,0.0057,0.0057,0.0058,0.0058,0.0059,0.0060,0.0061,0.0063,0.0067,0.0077},
					{0.0057,0.0057,0.0057,0.0058,0.0058,0.0059,0.0060,0.0061,0.0063,0.0066,0.0076},
					{0.0057,0.0057,0.0058,0.0058,0.0058,0.0059,0.0059,0.0060,0.0062,0.0065,0.0074},
					{0.0057,0.0058,0.0058,0.0058,0.0059,0.0059,0.0059,0.0060,0.0061,0.0064,0.0073},
					{0.0054,0.0054,0.0054,0.0055,0.0055,0.0056,0.0058,0.0059,0.0061,0.0065,0.0076},
					{0.0054,0.0054,0.0054,0.0054,0.0055,0.0056,0.0057,0.0059,0.0060,0.0065,0.0075},
					{0.0054,0.0053,0.0054,0.0054,0.0055,0.0055,0.0057,0.0058,0.0060,0.0064,0.0074},
					{0.0053,0.0054,0.0054,0.0054,0.0055,0.0055,0.0056,0.0058,0.0059,0.0063,0.0073},
					{0.0053,0.0054,0.0054,0.0054,0.0054,0.0055,0.0056,0.0057,0.0059,0.0062,0.0071},
					{0.0053,0.0054,0.0054,0.0054,0.0054,0.0055,0.0056,0.0057,0.0058,0.0061,0.0070},
					{0.0053,0.0053,0.0054,0.0054,0.0054,0.0054,0.0055,0.0056,0.0057,0.0061,0.0069},
					{0.0053,0.0054,0.0054,0.0054,0.0054,0.0054,0.0055,0.0056,0.0057,0.0060,0.0068},
					{0.0053,0.0054,0.0054,0.0054,0.0054,0.0054,0.0054,0.0055,0.0056,0.0059,0.0067},
					{0.0053,0.0053,0.0054,0.0054,0.0054,0.0054,0.0054,0.0055,0.0055,0.0058,0.0066},
					{0.0051,0.0051,0.0051,0.0051,0.0052,0.0053,0.0054,0.0055,0.0057,0.0060,0.0070},
					{0.0051,0.0051,0.0051,0.0051,0.0052,0.0052,0.0053,0.0055,0.0056,0.0060,0.0069},
					{0.0051,0.0051,0.0051,0.0051,0.0052,0.0052,0.0053,0.0054,0.0056,0.0059,0.0068},
					{0.0051,0.0051,0.0051,0.0051,0.0051,0.0052,0.0052,0.0054,0.0055,0.0058,0.0067},
					{0.0051,0.0051,0.0051,0.0051,0.0051,0.0051,0.0052,0.0053,0.0055,0.0058,0.0066},
					{0.0050,0.0051,0.0051,0.0050,0.0051,0.0051,0.0052,0.0053,0.0054,0.0057,0.0066},
					{0.0050,0.0050,0.0051,0.0050,0.0051,0.0051,0.0051,0.0052,0.0054,0.0056,0.0065},
					{0.0050,0.0050,0.0050,0.0050,0.0051,0.0051,0.0051,0.0052,0.0053,0.0056,0.0064},
					{0.0050,0.0050,0.0050,0.0050,0.0050,0.0050,0.0050,0.0051,0.0053,0.0055,0.0063},
					{0.0050,0.0050,0.0050,0.0050,0.0050,0.0050,0.0050,0.0051,0.0052,0.0054,0.0063},
					{0.0050,0.0049,0.0049,0.0050,0.0050,0.0051,0.0051,0.0052,0.0053,0.0057,0.0065},
					{0.0049,0.0049,0.0049,0.0050,0.0050,0.0050,0.0051,0.0052,0.0053,0.0057,0.0065},
					{0.0049,0.0049,0.0049,0.0049,0.0050,0.0050,0.0051,0.0052,0.0053,0.0056,0.0064},
					{0.0049,0.0049,0.0049,0.0049,0.0049,0.0050,0.0050,0.0051,0.0052,0.0056,0.0064},
					{0.0049,0.0048,0.0049,0.0049,0.0049,0.0050,0.0050,0.0051,0.0052,0.0056,0.0063},
					{0.0048,0.0048,0.0048,0.0049,0.0049,0.0049,0.0050,0.0050,0.0051,0.0055,0.0063},
					{0.0048,0.0048,0.0048,0.0048,0.0049,0.0049,0.0049,0.0050,0.0051,0.0055,0.0062},
					{0.0048,0.0048,0.0048,0.0048,0.0048,0.0049,0.0049,0.0050,0.0051,0.0054,0.0062},
					{0.0048,0.0047,0.0048,0.0048,0.0048,0.0048,0.0048,0.0049,0.0050,0.0054,0.0061},
					{0.0047,0.0047,0.0047,0.0048,0.0048,0.0048,0.0048,0.0049,0.0050,0.0053,0.0061}};
		
		double[] maturities = {0.5000,1.0000,1.5000,2.0000,2.5000,3.0000,3.5000,4.0000,4.5000,5.0000,
				5.5000,6.0000,6.5000,7.0000,7.5000,8.0000,8.5000,9.0000,9.5000,10.0000,
				10.5000,11.0000,11.5000,12.0000,12.5000,13.0000,13.5000,14.0000,14.5000,
				15.0000,15.5000,16.0000,16.5000,17.0000,17.5000,18.0000,18.5000,19.0000,
				19.5000,20.0000,20.5000,21.0000,21.5000,22.0000,22.5000,23.0000,23.5000,
				24.0000,24.5000,25.0000,25.5000,26.0000,26.5000,27.0000,27.5000,28.0000,
				28.5000,29.0000,29.5000};
		
		int[] indeces = {5,7,9,10,11,12,13,14,15,16,17,18,19}; //chose the index of the maturities we want to calibrate
		
		//Target caplet data
		QuotingConvention convention = QuotingConvention.VOLATILITYNORMAL;
		
		ArrayList<CapletSmileData> smiles = new ArrayList<CapletSmileData>();
		
		for(int i = 0; i<indeces.length; i++) {
			int index = indeces[i];
			double[] values = volatilities[index];
			double time = maturities[index];
			CapletSmileData ithSmile = new CapletSmileData("forward-EUR-6M","discount-EUR-OIS",referenceDate,strikes,time, values, convention);
			smiles.add(ithSmile);
		}
		
		CapletSmileData[] smileArray = new CapletSmileData[smiles.size()];
		smileArray = smiles.toArray(new CapletSmileData[smiles.size()]);
		
		CapletSurfaceData surface =  new CapletSurfaceData(smileArray, calibratedModel);
		
		OptimizerFactoryInterface optimizerFactory = new OptimizerFactoryLevenbergMarquardt(300 /* maxIterations */, 8 /* maxThreads */);

		
		double[] initialParameters = new double[] {0.1, 0.2,0.1, 0.3,0.1,0.7,0.4,0.6,0.1,0.2,0.1} /* initialParameters */;
		double[] parameterStep = new double[] {0.01,0.01,0.01,0.01,0.01,0.01,0.01,0.01,0.01,0.01, 0.01} /* parameterStep */;
		
		int numberOfPoints = 4096*16;
		double gridSpacing = 0.005;
		net.finmath.interpolation.RationalFunctionInterpolation.InterpolationMethod intMethod = net.finmath.interpolation.RationalFunctionInterpolation.InterpolationMethod.LINEAR;
		net.finmath.interpolation.RationalFunctionInterpolation.ExtrapolationMethod extMethod = net.finmath.interpolation.RationalFunctionInterpolation.ExtrapolationMethod.CONSTANT;
		
		CapletByCarrMadan pricer = new CapletByCarrMadan("forward-EUR-6M", 5.0,strikes,numberOfPoints,gridSpacing,intMethod,extMethod);
			
		/*double b = 0.3;
		double sigma = 0.1;
		double eta = 0.5;
		double zeta = 0.4;
		double alpha = 1.2;
		double[] initialValues = {0.01, 0.02};
		double[] immigrationRates = {0.1, 0.3};
		double[] lambda = {0.1, 0.2};*/
		
		double b = 0.948795792140507;
		double sigma = 0.08512087176733757;
		double eta = 0.8006437480710635;
		double zeta = 1.9;
		double alpha = 1.0596981657639444;
		double[] initialValues = {1.38347542293212884, 0.09406565308368039};
		double[] immigrationRates = {1.25824530309583305, 0.3740899227833204};
		double[] lambda = {1, 1};
	
		
		MultiCurveTenor threeMonth = new MultiCurveTenor(0.25, "3M");
		MultiCurveTenor sixMonth = new MultiCurveTenor(0.5, "6M");
		MultiCurveTenor[] tenors = {threeMonth,sixMonth};
		
		double timeHorizon = 12.0;		
		int numberOfTimeSteps = 50;
		
		CBIDrivenMultiCurveModel model = new CBIDrivenMultiCurveModel(timeHorizon, numberOfTimeSteps, calibratedModel, tenors, initialValues, immigrationRates, b, sigma, eta, zeta, alpha, lambda);
		
		CapletCalibrationProblem problem = new CapletCalibrationProblem(surface, model, optimizerFactory, pricer,initialParameters,parameterStep);
		
		System.out.println("Calibration started");
		
		long startMillis	= System.currentTimeMillis();
		net.finmath.fouriermethod.calibration.CapletCalibrationProblem.OptimizationResult result = problem.runCalibration();
		long endMillis		= System.currentTimeMillis();
		
		double calculationTime = ((endMillis-startMillis)/1000.0);
		
		System.out.println("Calibration completed in: " +calculationTime + " seconds");
		
		System.out.println("The solver required " + result.getIterations() + " iterations.");
		System.out.println("RMSQE " +result.getRootMeanSquaredError());
		
		double[] parameters = result.getModel().getParameters();
		for(int i =0; i<parameters.length; i++) {
			System.out.println(parameters[i]);
		}
		
		ArrayList<String> errorsOverview = result.getCalibrationOutput();
		
		for(String myString : errorsOverview)
			System.out.println(myString);
		
		System.out.println(result.getModel().getClass());
		
		
		System.out.println("Finished.");
	
		
	}
}