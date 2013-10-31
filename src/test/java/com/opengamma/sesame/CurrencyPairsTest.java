/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.sesame.FailureStatus.MISSING_DATA;
import static com.opengamma.sesame.SuccessStatus.SUCCESS;
import static com.opengamma.util.money.Currency.AUD;
import static com.opengamma.util.money.Currency.EUR;
import static com.opengamma.util.money.Currency.GBP;
import static com.opengamma.util.money.Currency.USD;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.util.test.TestGroup;


@Test(groups = TestGroup.UNIT)
public class CurrencyPairsTest {

  private CurrencyPairsFunction _pairsFunction;

  @BeforeMethod
  public void setUp() {

    ImmutableSet<CurrencyPair> pairs = ImmutableSet.of(
        CurrencyPair.of(EUR, USD), CurrencyPair.of(GBP, USD));
    _pairsFunction = new CurrencyPairs(pairs);
  }

  @Test
  public void testRetrieval() {
    FunctionResult<CurrencyPair> result = _pairsFunction.getCurrencyPair(EUR, USD);
    assertThat(result.getStatus(), is((ResultStatus) SUCCESS));
    assertThat(result.getResult(), is(CurrencyPair.of(EUR, USD)));
  }

  @Test
  public void testInverseRetrieval() {
    FunctionResult<CurrencyPair> result = _pairsFunction.getCurrencyPair(USD, GBP);
    assertThat(result.getStatus(), is((ResultStatus) SUCCESS));
    assertThat(result.getResult(), is(CurrencyPair.of(GBP, USD)));
  }

  @Test
  public void testUnknownPair() {
    FunctionResult<CurrencyPair> result = _pairsFunction.getCurrencyPair(AUD, GBP);
    assertThat(result.getStatus(), is((ResultStatus) MISSING_DATA));
  }

}
