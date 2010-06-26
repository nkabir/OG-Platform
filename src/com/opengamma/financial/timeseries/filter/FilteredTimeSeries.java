/**
 * Copyright (C) 2009 - 2009 by OpenGamma Inc.
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.timeseries.filter;

import org.apache.commons.lang.Validate;

import com.opengamma.util.timeseries.DoubleTimeSeries;

/**
 * 
 */
public class FilteredTimeSeries {
  private final DoubleTimeSeries<?> _filteredTS;
  private final DoubleTimeSeries<?> _rejectedTS;

  public FilteredTimeSeries(final DoubleTimeSeries<?> filteredTS, final DoubleTimeSeries<?> rejectedTS) {
    Validate.notNull(filteredTS, "filteredTS");
    Validate.notNull(rejectedTS, "rejectedTS");
    _filteredTS = filteredTS;
    _rejectedTS = rejectedTS;
  }

  public DoubleTimeSeries<?> getFilteredTS() {
    return _filteredTS;
  }

  public DoubleTimeSeries<?> getRejectedTS() {
    return _rejectedTS;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (_filteredTS == null ? 0 : _filteredTS.hashCode());
    result = prime * result + (_rejectedTS == null ? 0 : _rejectedTS.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final FilteredTimeSeries other = (FilteredTimeSeries) obj;
    if (_filteredTS == null) {
      if (other._filteredTS != null) {
        return false;
      }
    } else if (!_filteredTS.equals(other._filteredTS)) {
      return false;
    }
    if (_rejectedTS == null) {
      if (other._rejectedTS != null) {
        return false;
      }
    } else if (!_rejectedTS.equals(other._rejectedTS)) {
      return false;
    }
    return true;
  }
}
