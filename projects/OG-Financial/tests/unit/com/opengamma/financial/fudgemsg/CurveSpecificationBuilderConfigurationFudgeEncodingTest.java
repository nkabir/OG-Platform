/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.fudgemsg;

import static org.testng.AssertJUnit.assertEquals;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.opengamma.financial.analytics.ircurve.CurveDefinitionAndSpecifications;
import com.opengamma.financial.analytics.ircurve.CurveSpecificationBuilderConfiguration;

public class CurveSpecificationBuilderConfigurationFudgeEncodingTest extends FinancialTestBase {

  private static final Logger s_logger = LoggerFactory.getLogger(CurveSpecificationBuilderConfigurationFudgeEncodingTest.class);
  @Test
  public void testCycle() {
    CurveSpecificationBuilderConfiguration configuration = CurveDefinitionAndSpecifications.buildTestConfiguration();
    CurveSpecificationBuilderConfiguration cycleObject = cycleObject(CurveSpecificationBuilderConfiguration.class, configuration);
    s_logger.error(configuration.toString());
    s_logger.error(cycleObject.toString());
    assertEquals(configuration, cycleObject(CurveSpecificationBuilderConfiguration.class, configuration));
  }

}
