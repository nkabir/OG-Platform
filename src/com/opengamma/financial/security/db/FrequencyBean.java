/**
 * Copyright (C) 2009 - 2010 by OpenGamma Inc.
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.security.db;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.financial.convention.frequency.Frequency;
import com.opengamma.financial.convention.frequency.FrequencyFactory;

public class FrequencyBean extends EnumBean {
  
  protected FrequencyBean() {
  }

  public FrequencyBean(String frequency) {
    super(frequency);
  }
  
  /* package */ Frequency toFrequency() {
    final Frequency f = FrequencyFactory.INSTANCE.getFrequency(getName());
    if (f == null) {
      throw new OpenGammaRuntimeException("Bad value for frequencyBean (" + getName() + ")");
    }
    return f;
  }
  
}
