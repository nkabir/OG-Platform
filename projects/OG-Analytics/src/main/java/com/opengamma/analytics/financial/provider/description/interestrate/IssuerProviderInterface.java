/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.analytics.financial.provider.description.interestrate;

import java.util.List;
import java.util.Set;

import com.opengamma.analytics.financial.legalentity.LegalEntity;
import com.opengamma.analytics.financial.legalentity.LegalEntityMeta;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.ForwardSensitivity;
import com.opengamma.util.tuple.DoublesPair;
import com.opengamma.util.tuple.Pair;

/**
 * Interface for issuer specific
 */
public interface IssuerProviderInterface extends ParameterIssuerProviderInterface {
  // TODO: Can probably be merged with MulticurveProvider if the Currency is replaced by a UniqueIdentifiable.

  @Override
  IssuerProviderInterface copy();

  /**
   * Gets the discount factor for one issuer in one currency.
   * @param issuerCcy The issuer name/currency pair.
   * @param time The time.
   * @return The discount factor.
   */
  double getDiscountFactor(LegalEntity issuer, Double time);

//  /**
//   * Return the name associated to the discounting for a issuer/currency.
//   * @param issuerCcy The issuer/currency.
//   * @return The name.
//   */
//  String getName(Pair<String, Currency> issuerCcy);

  /**
  * Return the name associated to the discounting for a issuer/currency.
  * @param issuerCcy The issuer/currency.
  * @return The name.
  */
  String getName(Pair<Object, LegalEntityMeta<LegalEntity>> issuer);

  /**
  * Return the name associated to the discounting for a issuer/currency.
  * @param issuerCcy The issuer/currency.
  * @return The name.
  */
  String getName(LegalEntity issuer);

  /**
   * Gets the names of all curves (discounting, forward, and issuers).
   * @return The names.
   */
  Set<String> getAllNames();

  /**
   * Returns the MulticurveProvider from which the IssuerProvider is composed.
   * @return The multi-curves provider.
   */
  @Override
  MulticurveProviderInterface getMulticurveProvider();

  double[] parameterSensitivity(String name, List<DoublesPair> pointSensitivity);

  double[] parameterForwardSensitivity(String name, List<ForwardSensitivity> pointSensitivity);

  Integer getNumberOfParameters(String name);

  List<String> getUnderlyingCurvesNames(String name);

  //@Deprecated
  //Set<Pair<String, Currency>> getIssuersCurrencies();

  Set<Pair<Object, LegalEntityMeta<LegalEntity>>> getIssuers();
}
