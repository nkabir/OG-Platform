/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.analytics.model.curve;

import static com.opengamma.engine.value.ValueRequirementNames.HULL_WHITE_ONE_FACTOR_PARAMETERS;
import static com.opengamma.financial.analytics.model.curve.CurveCalculationPropertyNamesAndValues.PROPERTY_HULL_WHITE_PARAMETERS;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.Clock;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.analytics.financial.model.interestrate.definition.HullWhiteOneFactorPiecewiseConstantParameters;
import com.opengamma.analytics.util.time.TimeCalculator;
import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.config.impl.ConfigItem;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.engine.ComputationTarget;
import com.opengamma.engine.ComputationTargetSpecification;
import com.opengamma.engine.function.AbstractFunction;
import com.opengamma.engine.function.CompiledFunctionDefinition;
import com.opengamma.engine.function.FunctionCompilationContext;
import com.opengamma.engine.function.FunctionExecutionContext;
import com.opengamma.engine.function.FunctionInputs;
import com.opengamma.engine.target.ComputationTargetType;
import com.opengamma.engine.value.ComputedValue;
import com.opengamma.engine.value.ValueProperties;
import com.opengamma.engine.value.ValueRequirement;
import com.opengamma.engine.value.ValueSpecification;
import com.opengamma.financial.OpenGammaCompilationContext;
import com.opengamma.financial.analytics.parameters.HullWhiteOneFactorParameters;
import com.opengamma.financial.view.ConfigDocumentWatchSetProvider;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalScheme;
import com.opengamma.id.VersionCorrection;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.async.AsynchronousExecution;
import com.opengamma.util.money.Currency;
import com.opengamma.util.time.Tenor;

/**
 * Function that supplies Hull-White one factor parameters.
 */
public class HullWhiteOneFactorParametersFunction extends AbstractFunction {
  /** The logger */
  private static final Logger s_logger = LoggerFactory.getLogger(HullWhiteOneFactorParametersFunction.class);
  /** The configuration name */
  private final String _name;
  /** The currency for which these parameters are valid */
  private final Currency _currency;
  /** The set of results of this function */
  private final ValueSpecification _result;

  /**
   * @param name The name of the Hull-White parameter set, not null
   * @param currency The currency for which the parameters are valid, not null
   */
  public HullWhiteOneFactorParametersFunction(final String name, final String currency) {
    ArgumentChecker.notNull(name, "name");
    ArgumentChecker.notNull(currency, "currency");
    _name = name;
    _currency = Currency.of(currency);
    final ValueProperties properties = createValueProperties()
        .with(PROPERTY_HULL_WHITE_PARAMETERS, _name)
        .get();
    _result = new ValueSpecification(HULL_WHITE_ONE_FACTOR_PARAMETERS, ComputationTargetSpecification.of(_currency), properties);
  }

  @Override
  public void init(final FunctionCompilationContext context) {
    ConfigDocumentWatchSetProvider.reinitOnChanges(context, this, HullWhiteOneFactorParameters.class);
  }

  @Override
  public CompiledFunctionDefinition compile(final FunctionCompilationContext context, final Instant atInstant) {
    final Set<ValueRequirement> requirements = new HashSet<>();
    final ConfigSource configSource = OpenGammaCompilationContext.getConfigSource(context);
    final Collection<ConfigItem<HullWhiteOneFactorParameters>> configs = configSource.get(HullWhiteOneFactorParameters.class, _name, VersionCorrection.LATEST);
    if (configs == null) {
      throw new OpenGammaRuntimeException("HullWhiteOneFactorParameter configuration called " + _name + " was null");
    }
    final HullWhiteOneFactorParameters parameters = configs.iterator().next().getValue();
    requirements.add(new ValueRequirement(MarketDataRequirementNames.MARKET_VALUE, ComputationTargetType.PRIMITIVE, parameters.getMeanReversionId()));
    requirements.add(new ValueRequirement(MarketDataRequirementNames.MARKET_VALUE, ComputationTargetType.PRIMITIVE, parameters.getInitialVolatilityId()));
    final Map<Tenor, ExternalId> volatilityTermStructure = parameters.getVolatilityTermStructure();
    for (final Map.Entry<Tenor, ExternalId> entry : volatilityTermStructure.entrySet()) {
      final ExternalScheme scheme = entry.getValue().getScheme();
      final String id = entry.getValue().getValue();
      final ExternalId tenorAppendedId = ExternalId.of(scheme, createId(entry.getKey(), id));
      requirements.add(new ValueRequirement(MarketDataRequirementNames.MARKET_VALUE, ComputationTargetType.PRIMITIVE, tenorAppendedId));
    }
    final ZonedDateTime atZDT = ZonedDateTime.ofInstant(atInstant, ZoneOffset.UTC);
    return new AbstractInvokingCompiledFunction(atZDT.with(LocalTime.MIDNIGHT), atZDT.plusDays(1).with(LocalTime.MIDNIGHT).minusNanos(1000000)) {

      @Override
      public Set<ComputedValue> execute(final FunctionExecutionContext executionContext, final FunctionInputs inputs, final ComputationTarget target,
          final Set<ValueRequirement> desiredValues) throws AsynchronousExecution {
        final Clock snapshotClock = executionContext.getValuationClock();
        final ZonedDateTime now = ZonedDateTime.now(snapshotClock);
        final Object meanReversionObject = inputs.getValue(new ValueRequirement(MarketDataRequirementNames.MARKET_VALUE,
            ComputationTargetType.PRIMITIVE, parameters.getMeanReversionId()));
        if (meanReversionObject == null) {
          throw new OpenGammaRuntimeException("Could not get mean reversion value");
        }
        final Object initialVolatilityObject = inputs.getValue(new ValueRequirement(MarketDataRequirementNames.MARKET_VALUE,
            ComputationTargetType.PRIMITIVE, parameters.getInitialVolatilityId()));
        if (initialVolatilityObject == null) {
          throw new OpenGammaRuntimeException("Could not get initial volatility value");
        }
        final Double meanReversion = (Double) meanReversionObject;
        final Double initialVolatility = (Double) initialVolatilityObject;
        final DoubleArrayList volatility = new DoubleArrayList();
        volatility.add(initialVolatility);
        final DoubleArrayList volatilityTime = new DoubleArrayList();
        for (final Map.Entry<Tenor, ExternalId> entry : volatilityTermStructure.entrySet()) {
          final ExternalScheme scheme = entry.getValue().getScheme();
          final String id = entry.getValue().getValue();
          final ExternalId tenorAppendedId = ExternalId.of(scheme, createId(entry.getKey(), id));
          final Object volatilityObject = inputs.getValue(new ValueRequirement(MarketDataRequirementNames.MARKET_VALUE,
              ComputationTargetType.PRIMITIVE, tenorAppendedId));
          if (volatilityObject == null) {
            s_logger.error("Could not get value for " + tenorAppendedId);
          } else {
            final double t = TimeCalculator.getTimeBetween(now, now.plus(entry.getKey().getPeriod()));
            volatility.add((Double) volatilityObject);
            volatilityTime.add(t);
          }
        }
        final HullWhiteOneFactorPiecewiseConstantParameters hullWhiteParameters = new HullWhiteOneFactorPiecewiseConstantParameters(0, volatility.toDoubleArray(),
            volatilityTime.toDoubleArray());
        return Collections.singleton(new ComputedValue(_result, hullWhiteParameters));
      }

      @Override
      public ComputationTargetType getTargetType() {
        return ComputationTargetType.CURRENCY;
      }

      @Override
      public boolean canApplyTo(final FunctionCompilationContext compilationContext, final ComputationTarget target) {
        return _currency.equals(target.getValue());
      }

      @Override
      public Set<ValueSpecification> getResults(final FunctionCompilationContext compilationContext, final ComputationTarget target) {
        return Collections.singleton(_result);
      }

      @Override
      public Set<ValueRequirement> getRequirements(final FunctionCompilationContext compilationContext, final ComputationTarget target, final ValueRequirement desiredValue) {
        final ValueProperties constraints = desiredValue.getConstraints();
        final Set<String> names = constraints.getValues(PROPERTY_HULL_WHITE_PARAMETERS);
        if (names == null || names.size() != 1) {
          return null;
        }
        return requirements;
      }

      @Override
      public boolean canHandleMissingRequirements() {
        return true;
      }

      @Override
      public boolean canHandleMissingInputs() {
        return true;
      }

    };
  }

  static String createId(final Tenor tenor, final String id) {
    final StringBuilder newId = new StringBuilder(id);
    newId.append("_");
    newId.append(tenor.getPeriod().toString());
    return newId.toString();
  }

}
