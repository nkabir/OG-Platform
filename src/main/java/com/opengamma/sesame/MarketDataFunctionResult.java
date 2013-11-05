/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.Map;

import com.opengamma.core.marketdatasnapshot.SnapshotDataBundle;
import com.opengamma.util.tuple.Pair;

public interface MarketDataFunctionResult extends FunctionResult<Map<MarketDataRequirement, Pair<MarketDataStatus, ? extends MarketDataValue>>> {

  MarketDataValue getSingleMarketDataValue();

  MarketDataStatus getMarketDataState(MarketDataRequirement requirement);

  MarketDataValue getMarketDataValue(MarketDataRequirement requirement);

  /**
   * Temporary method to allow conversion to the old-style market data bundle.
   *
   * @return a snapshot bundle with the data from the result
   */
  SnapshotDataBundle toSnapshot();
}
