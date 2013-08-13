/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.analytics.conversion;

import static com.opengamma.financial.convention.percurrency.PerCurrencyConventionHelper.IBOR;
import static com.opengamma.financial.convention.percurrency.PerCurrencyConventionHelper.IRS_FIXED_LEG;
import static com.opengamma.financial.convention.percurrency.PerCurrencyConventionHelper.IRS_IBOR_LEG;
import static com.opengamma.financial.convention.percurrency.PerCurrencyConventionHelper.LIBOR;
import static com.opengamma.financial.convention.percurrency.PerCurrencyConventionHelper.OVERNIGHT;
import static com.opengamma.financial.convention.percurrency.PerCurrencyConventionHelper.SCHEME_NAME;
import static com.opengamma.financial.convention.percurrency.PerCurrencyConventionHelper.getConventionName;

import org.threeten.bp.Period;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.analytics.financial.instrument.InstrumentDefinition;
import com.opengamma.analytics.financial.instrument.annuity.AnnuityCouponFixedDefinition;
import com.opengamma.analytics.financial.instrument.annuity.AnnuityCouponIborDefinition;
import com.opengamma.analytics.financial.instrument.annuity.AnnuityCouponIborSpreadDefinition;
import com.opengamma.analytics.financial.instrument.annuity.AnnuityDefinition;
import com.opengamma.analytics.financial.instrument.index.GeneratorSwapFixedON;
import com.opengamma.analytics.financial.instrument.index.IborIndex;
import com.opengamma.analytics.financial.instrument.index.IndexON;
import com.opengamma.analytics.financial.instrument.payment.PaymentDefinition;
import com.opengamma.analytics.financial.instrument.swap.SwapDefinition;
import com.opengamma.analytics.financial.instrument.swap.SwapFixedIborDefinition;
import com.opengamma.analytics.financial.instrument.swap.SwapFixedIborSpreadDefinition;
import com.opengamma.analytics.financial.instrument.swap.SwapFixedONDefinition;
import com.opengamma.analytics.financial.instrument.swap.SwapFixedONSimplifiedDefinition;
import com.opengamma.core.holiday.HolidaySource;
import com.opengamma.core.region.RegionSource;
import com.opengamma.financial.analytics.fixedincome.InterestRateInstrumentType;
import com.opengamma.financial.convention.ConventionSource;
import com.opengamma.financial.convention.IborIndexConvention;
import com.opengamma.financial.convention.OvernightIndexConvention;
import com.opengamma.financial.convention.SwapFixedLegConvention;
import com.opengamma.financial.convention.VanillaIborLegConvention;
import com.opengamma.financial.convention.businessday.BusinessDayConvention;
import com.opengamma.financial.convention.calendar.Calendar;
import com.opengamma.financial.convention.daycount.DayCount;
import com.opengamma.financial.convention.frequency.Frequency;
import com.opengamma.financial.convention.frequency.PeriodFrequency;
import com.opengamma.financial.convention.frequency.SimpleFrequency;
import com.opengamma.financial.security.FinancialSecurityVisitorAdapter;
import com.opengamma.financial.security.swap.FixedInflationSwapLeg;
import com.opengamma.financial.security.swap.FixedInterestRateLeg;
import com.opengamma.financial.security.swap.FixedVarianceSwapLeg;
import com.opengamma.financial.security.swap.FloatingGearingIRLeg;
import com.opengamma.financial.security.swap.FloatingInterestRateLeg;
import com.opengamma.financial.security.swap.FloatingSpreadIRLeg;
import com.opengamma.financial.security.swap.FloatingVarianceSwapLeg;
import com.opengamma.financial.security.swap.ForwardSwapSecurity;
import com.opengamma.financial.security.swap.InflationIndexSwapLeg;
import com.opengamma.financial.security.swap.InterestRateNotional;
import com.opengamma.financial.security.swap.SwapLeg;
import com.opengamma.financial.security.swap.SwapLegVisitor;
import com.opengamma.financial.security.swap.SwapSecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.Currency;

/**
 *
 */
public class SwapSecurityConverter extends FinancialSecurityVisitorAdapter<InstrumentDefinition<?>> {
  /** A holiday source */
  private final HolidaySource _holidaySource;
  /** A convention bundle source */
  private final ConventionSource _conventionSource;
  /** A region source */
  private final RegionSource _regionSource;
  /** Is this converter being used in curve construction code */
  private final boolean _forCurves;

  /**
   * @param holidaySource The holiday source, not null
   * @param conventionSource The convention source, not null
   * @param regionSource The region source, not null
   * @param forCurves true if the converter is used in curve construction code
   */
  public SwapSecurityConverter(final HolidaySource holidaySource, final ConventionSource conventionSource, final RegionSource regionSource,
      final boolean forCurves) {
    ArgumentChecker.notNull(holidaySource, "holiday source");
    ArgumentChecker.notNull(conventionSource, "convention source");
    ArgumentChecker.notNull(regionSource, "region source");
    _holidaySource = holidaySource;
    _conventionSource = conventionSource;
    _regionSource = regionSource;
    _forCurves = forCurves;
  }

  @Override
  public InstrumentDefinition<?> visitForwardSwapSecurity(final ForwardSwapSecurity security) {
    return visitSwapSecurity(security);
  }

  @Override
  public InstrumentDefinition<?> visitSwapSecurity(final SwapSecurity security) {
    ArgumentChecker.notNull(security, "swap security");
    final InterestRateInstrumentType swapType = SwapSecurityUtils.getSwapType(security);
    switch (swapType) {
      case SWAP_FIXED_IBOR:
        return getFixedIborSwapDefinition(security, SwapSecurityUtils.payFixed(security), false);
      case SWAP_FIXED_IBOR_WITH_SPREAD:
        return getFixedIborSwapDefinition(security, SwapSecurityUtils.payFixed(security), true);
      case SWAP_FIXED_OIS:
        return getFixedOISSwapDefinition(security, SwapSecurityUtils.payFixed(security), _forCurves);
      default:
        final ZonedDateTime effectiveDate = security.getEffectiveDate();
        final ZonedDateTime maturityDate = security.getMaturityDate();
        final AnnuityDefinition<? extends PaymentDefinition> payLeg = security.getPayLeg().accept(getSwapLegConverter(effectiveDate, maturityDate, true));
        final AnnuityDefinition<? extends PaymentDefinition> receiveLeg = security.getReceiveLeg().accept(getSwapLegConverter(effectiveDate, maturityDate, false));
        return new SwapDefinition(payLeg, receiveLeg);
    }
  }

  private SwapDefinition getFixedIborSwapDefinition(final SwapSecurity swapSecurity, final boolean payFixed, final boolean hasSpread) {
    final ZonedDateTime effectiveDate = swapSecurity.getEffectiveDate();
    final ZonedDateTime maturityDate = swapSecurity.getMaturityDate();
    final SwapLeg payLeg = swapSecurity.getPayLeg();
    final SwapLeg receiveLeg = swapSecurity.getReceiveLeg();
    final FixedInterestRateLeg fixedLeg = (FixedInterestRateLeg) (payFixed ? payLeg : receiveLeg);
    final FloatingInterestRateLeg iborLeg = (FloatingInterestRateLeg) (payFixed ? receiveLeg : payLeg);
    final ExternalId regionId = payLeg.getRegionId();
    final Calendar calendar = CalendarUtils.getCalendar(_regionSource, _holidaySource, regionId);
    final Currency currency = ((InterestRateNotional) payLeg.getNotional()).getCurrency();
    String iborConventionName = getConventionName(currency, IBOR);
    IborIndexConvention iborIndexConvention = _conventionSource.getConvention(IborIndexConvention.class, ExternalId.of(SCHEME_NAME, iborConventionName));
    if (iborIndexConvention == null) {
      iborConventionName = getConventionName(currency, LIBOR); //TODO shouldn't just use Libor
      iborIndexConvention = _conventionSource.getConvention(IborIndexConvention.class, ExternalId.of(SCHEME_NAME, iborConventionName));
      if (iborIndexConvention == null) {
        throw new OpenGammaRuntimeException("Could not get Ibor index convention with the identifier " + ExternalId.of(SCHEME_NAME, iborConventionName));
      }
    }
    final Frequency freqIbor = iborLeg.getFrequency();
    final Period tenorIbor = getTenor(freqIbor);
    final int spotLag = iborIndexConvention.getSettlementDays();
    final IborIndex indexIbor = new IborIndex(currency, tenorIbor, spotLag, iborIndexConvention.getDayCount(),
        iborIndexConvention.getBusinessDayConvention(), iborIndexConvention.isIsEOM(), iborIndexConvention.getName());
    final Frequency freqFixed = fixedLeg.getFrequency();
    final Period tenorFixed = getTenor(freqFixed);
    final double fixedLegNotional = ((InterestRateNotional) fixedLeg.getNotional()).getAmount();
    final double iborLegNotional = ((InterestRateNotional) iborLeg.getNotional()).getAmount();
    if (hasSpread) {
      final double spread = ((FloatingSpreadIRLeg) iborLeg).getSpread();
      return SwapFixedIborSpreadDefinition.from(effectiveDate, maturityDate, tenorFixed, fixedLeg.getDayCount(), fixedLeg.getBusinessDayConvention(), fixedLeg.isEom(), fixedLegNotional,
          fixedLeg.getRate(), tenorIbor, iborLeg.getDayCount(), iborLeg.getBusinessDayConvention(), iborLeg.isEom(), iborLegNotional, indexIbor, spread, payFixed, calendar);
    }
    final SwapFixedIborDefinition swap = SwapFixedIborDefinition.from(effectiveDate, maturityDate, tenorFixed, fixedLeg.getDayCount(), fixedLeg.getBusinessDayConvention(), fixedLeg.isEom(),
        fixedLegNotional, fixedLeg.getRate(), tenorIbor, iborLeg.getDayCount(), iborLeg.getBusinessDayConvention(), iborLeg.isEom(), iborLegNotional, indexIbor, payFixed, calendar);
    return swap;
  }

  private SwapDefinition getFixedOISSwapDefinition(final SwapSecurity swapSecurity, final boolean payFixed, final boolean forCurve) {
    final ZonedDateTime effectiveDate = swapSecurity.getEffectiveDate();
    final ZonedDateTime maturityDate = swapSecurity.getMaturityDate();
    final SwapLeg payLeg = swapSecurity.getPayLeg();
    final SwapLeg receiveLeg = swapSecurity.getReceiveLeg();
    final FixedInterestRateLeg fixedLeg = (FixedInterestRateLeg) (payFixed ? payLeg : receiveLeg);
    final FloatingInterestRateLeg floatLeg = (FloatingInterestRateLeg) (payFixed ? receiveLeg : payLeg);
    final Currency currency = ((InterestRateNotional) payLeg.getNotional()).getCurrency();
    final String overnightConventionName = getConventionName(currency, OVERNIGHT);
    final OvernightIndexConvention indexConvention = _conventionSource.getConvention(OvernightIndexConvention.class, ExternalId.of(SCHEME_NAME, overnightConventionName));
    if (indexConvention == null) {
      throw new OpenGammaRuntimeException("Could not get OIS index convention with the identifier " + ExternalId.of(SCHEME_NAME, overnightConventionName));
    }
    final Calendar calendar = CalendarUtils.getCalendar(_regionSource, _holidaySource, indexConvention.getRegionCalendar());
    final String currencyString = currency.getCode();
    final Integer publicationLag = indexConvention.getPublicationLag();
    final Period paymentFrequency = getTenor(floatLeg.getFrequency());
    final IndexON index = new IndexON(floatLeg.getFloatingReferenceRateId().getValue(), currency, indexConvention.getDayCount(), publicationLag);
    final GeneratorSwapFixedON generator = new GeneratorSwapFixedON(currencyString + "_OIS_Convention", index, paymentFrequency, fixedLeg.getDayCount(), fixedLeg.getBusinessDayConvention(),
        fixedLeg.isEom(), 0, 1 - publicationLag, calendar); // TODO: The payment lag is not available at the security level!
    final double notionalFixed = ((InterestRateNotional) fixedLeg.getNotional()).getAmount();
    final double notionalOIS = ((InterestRateNotional) floatLeg.getNotional()).getAmount();
    if (forCurve) {
      return SwapFixedONSimplifiedDefinition.from(effectiveDate, maturityDate, notionalFixed, notionalOIS, generator, fixedLeg.getRate(), payFixed);
    }
    return SwapFixedONDefinition.from(effectiveDate, maturityDate, notionalFixed, notionalOIS, generator, fixedLeg.getRate(), payFixed);
  }

  private static Period getTenor(final Frequency freq) {
    if (freq instanceof PeriodFrequency) {
      return ((PeriodFrequency) freq).getPeriod();
    } else if (freq instanceof SimpleFrequency) {
      return ((SimpleFrequency) freq).toPeriodFrequency().getPeriod();
    }
    throw new OpenGammaRuntimeException("Can only PeriodFrequency or SimpleFrequency; have " + freq.getClass());
  }

  private static String getTenorString(final Frequency freq) {
    final Period period;
    if (freq instanceof PeriodFrequency) {
      period = ((PeriodFrequency) freq).getPeriod();
    } else if (freq instanceof SimpleFrequency) {
      period = ((SimpleFrequency) freq).toPeriodFrequency().getPeriod();
    } else {
      throw new OpenGammaRuntimeException("Can only PeriodFrequency or SimpleFrequency; have " + freq.getClass());
    }
    return period.toString().substring(1, period.toString().length());
  }

  private SwapLegVisitor<AnnuityDefinition<? extends PaymentDefinition>> getSwapLegConverter(final ZonedDateTime effectiveDate, final ZonedDateTime maturityDate, final boolean isPayer) {
    return new SwapLegVisitor<AnnuityDefinition<? extends PaymentDefinition>>() {

      @Override
      public final AnnuityDefinition<? extends PaymentDefinition> visitFixedInterestRateLeg(final FixedInterestRateLeg swapLeg) {
        final ExternalId regionId = swapLeg.getRegionId();
        final Calendar calendar = CalendarUtils.getCalendar(_regionSource, _holidaySource, regionId);
        final InterestRateNotional interestRateNotional = (InterestRateNotional) swapLeg.getNotional();
        final Currency currency = interestRateNotional.getCurrency();
        final String fixedLegConventionName = getConventionName(currency, IRS_FIXED_LEG);
        final SwapFixedLegConvention fixedLegConvention = _conventionSource.getConvention(SwapFixedLegConvention.class, ExternalId.of(SCHEME_NAME, fixedLegConventionName));
        if (fixedLegConvention == null) {
          throw new OpenGammaRuntimeException("Could not get fixed leg convention with the identifier " + ExternalId.of(SCHEME_NAME, fixedLegConventionName));
        }
        final Frequency freqFixed = swapLeg.getFrequency();
        final Period tenorFixed = getTenor(freqFixed);
        final double notional = interestRateNotional.getAmount();
        final DayCount dayCount = fixedLegConvention.getDayCount();
        final boolean isEOM = fixedLegConvention.isIsEOM();
        final double rate = swapLeg.getRate();
        final BusinessDayConvention businessDayConvention = fixedLegConvention.getBusinessDayConvention();
        return AnnuityCouponFixedDefinition.from(currency, effectiveDate, maturityDate, tenorFixed, calendar, dayCount,
            businessDayConvention, isEOM, notional, rate, isPayer);
      }

      @Override
      public final AnnuityDefinition<? extends PaymentDefinition> visitFloatingInterestRateLeg(final FloatingInterestRateLeg swapLeg) {
        final InterestRateNotional interestRateNotional = (InterestRateNotional) swapLeg.getNotional();
        final Currency currency = interestRateNotional.getCurrency();
        final Calendar calendar = CalendarUtils.getCalendar(_regionSource, _holidaySource, swapLeg.getRegionId());
        switch (swapLeg.getFloatingRateType()) {
          case IBOR:
            final String tenorString = getTenorString(swapLeg.getFrequency());
            final String iborLegConventionName = getConventionName(currency, tenorString, IRS_IBOR_LEG);
            final VanillaIborLegConvention iborLegConvention = _conventionSource.getConvention(VanillaIborLegConvention.class, ExternalId.of(SCHEME_NAME, iborLegConventionName));
            if (iborLegConvention == null) {
              throw new OpenGammaRuntimeException("Could not get Ibor leg convention with the identifier " + ExternalId.of(SCHEME_NAME, iborLegConventionName));
            }
            final IborIndexConvention iborIndexConvention = _conventionSource.getConvention(IborIndexConvention.class, iborLegConvention.getIborIndexConvention());
            final Frequency freqIbor = swapLeg.getFrequency();
            final Period tenorIbor = getTenor(freqIbor);
            final int spotLag = iborIndexConvention.getSettlementDays();
            final DayCount dayCount = swapLeg.getDayCount();
            final BusinessDayConvention businessDayConvention = swapLeg.getBusinessDayConvention();
            final double notional = interestRateNotional.getAmount();
            final IborIndex iborIndex = new IborIndex(currency, tenorIbor, spotLag, iborIndexConvention.getDayCount(), iborIndexConvention.getBusinessDayConvention(),
                iborIndexConvention.isIsEOM(), iborIndexConvention.getName());
            return AnnuityCouponIborDefinition.from(effectiveDate, maturityDate, tenorIbor, notional, iborIndex, isPayer, businessDayConvention, swapLeg.isEom(), dayCount,
                calendar);
          default:
            throw new OpenGammaRuntimeException("Cannot handle floating type " + swapLeg.getFloatingRateType());
        }
      }

      @Override
      public final AnnuityDefinition<? extends PaymentDefinition> visitFloatingSpreadIRLeg(final FloatingSpreadIRLeg swapLeg) {
        final InterestRateNotional interestRateNotional = (InterestRateNotional) swapLeg.getNotional();
        final Currency currency = interestRateNotional.getCurrency();
        final Calendar calendar = CalendarUtils.getCalendar(_regionSource, _holidaySource, swapLeg.getRegionId());
        switch (swapLeg.getFloatingRateType()) {
          case IBOR:
            final String tenorString = getTenorString(swapLeg.getFrequency());
            final String iborLegConventionName = getConventionName(currency, tenorString, IRS_IBOR_LEG);
            final VanillaIborLegConvention iborLegConvention = _conventionSource.getConvention(VanillaIborLegConvention.class, ExternalId.of(SCHEME_NAME, iborLegConventionName));
            if (iborLegConvention == null) {
              throw new OpenGammaRuntimeException("Could not get Ibor leg convention with the identifier " + ExternalId.of(SCHEME_NAME, iborLegConventionName));
            }
            final IborIndexConvention iborIndexConvention = _conventionSource.getConvention(IborIndexConvention.class, iborLegConvention.getIborIndexConvention());
            final Frequency freqIbor = swapLeg.getFrequency();
            final Period tenorIbor = getTenor(freqIbor);
            final int spotLag = iborIndexConvention.getSettlementDays();
            final DayCount dayCount = swapLeg.getDayCount();
            final BusinessDayConvention businessDayConvention = swapLeg.getBusinessDayConvention();
            final double notional = interestRateNotional.getAmount();
            final IborIndex iborIndex = new IborIndex(currency, tenorIbor, spotLag, iborIndexConvention.getDayCount(), iborIndexConvention.getBusinessDayConvention(),
                iborIndexConvention.isIsEOM(), iborIndexConvention.getName());
            final double spread = swapLeg.getSpread();
            return AnnuityCouponIborSpreadDefinition.from(effectiveDate, maturityDate, tenorIbor, notional, iborIndex, isPayer, businessDayConvention, swapLeg.isEom(), dayCount,
                spread, calendar);
          default:
            throw new OpenGammaRuntimeException("Cannot handle floating type " + swapLeg.getFloatingRateType());
        }
      }

      @Override
      public final AnnuityDefinition<? extends PaymentDefinition> visitFloatingGearingIRLeg(final FloatingGearingIRLeg swapLeg) {
        throw new OpenGammaRuntimeException("Cannot handle " + swapLeg.getClass());
      }

      @Override
      public final AnnuityDefinition<? extends PaymentDefinition> visitFixedVarianceSwapLeg(final FixedVarianceSwapLeg swapLeg) {
        throw new OpenGammaRuntimeException("Cannot handle " + swapLeg.getClass());
      }

      @Override
      public final AnnuityDefinition<? extends PaymentDefinition> visitFloatingVarianceSwapLeg(final FloatingVarianceSwapLeg swapLeg) {
        throw new OpenGammaRuntimeException("Cannot handle " + swapLeg.getClass());
      }

      @Override
      public final AnnuityDefinition<? extends PaymentDefinition> visitFixedInflationSwapLeg(final FixedInflationSwapLeg swapLeg) {
        throw new OpenGammaRuntimeException("Cannot handle " + swapLeg.getClass());
      }

      @Override
      public final AnnuityDefinition<? extends PaymentDefinition> visitInflationIndexSwapLeg(final InflationIndexSwapLeg swapLeg) {
        throw new OpenGammaRuntimeException("Cannot handle " + swapLeg.getClass());
      }
    };
  }
}
