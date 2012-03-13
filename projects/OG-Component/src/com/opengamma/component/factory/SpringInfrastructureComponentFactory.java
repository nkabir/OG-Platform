/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.component.factory;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.management.ManagementService;

import org.fudgemsg.FudgeContext;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.impl.direct.DirectBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.springframework.context.Lifecycle;
import org.springframework.context.support.GenericApplicationContext;

import com.opengamma.component.ComponentFactory;
import com.opengamma.component.ComponentRepository;
import com.opengamma.util.db.DbConnector;
import com.opengamma.util.jms.JmsConnector;
import com.opengamma.util.mongo.MongoConnector;

/**
 * Component definition for the infrastructure defined in Spring.
 */
@BeanDefinition
public class SpringInfrastructureComponentFactory extends AbstractSpringComponentFactory implements ComponentFactory {

  @Override
  public void init(ComponentRepository repo, LinkedHashMap<String, String> configuration) {
    GenericApplicationContext appContext = createApplicationContext(repo);
    register(repo, appContext);
  }

  /**
   * Registers the infrastructure components.
   * 
   * @param repo  the repository to register with, not null
   * @param appContext  the Spring application context, not null
   */
  protected void register(ComponentRepository repo, GenericApplicationContext appContext) {
    registerInfrastructureByType(repo, DbConnector.class, appContext);
    registerInfrastructureByType(repo, MongoConnector.class, appContext);
    registerInfrastructureByType(repo, JmsConnector.class, appContext);
    registerInfrastructureByType(repo, FudgeContext.class, appContext);
    registerInfrastructureByType(repo, CacheManager.class, appContext);
    registerInfrastructureByType(repo, ScheduledExecutorService.class, appContext);
    registerInfrastructureByType(repo, MBeanServer.class, appContext);
    
    registerJmxCacheManager(repo);
  }

  protected void registerJmxCacheManager(ComponentRepository repo) {
    MBeanServer jmx = repo.findInstance(MBeanServer.class);
    if (jmx != null) {
      Set<CacheManager> set = Collections.newSetFromMap(new IdentityHashMap<CacheManager, Boolean>());
      set.addAll(repo.getInstances(CacheManager.class));
      for (CacheManager mgr : set) {
        ManagementService jmxService = new ManagementService(mgr, jmx, true, true, true, true);
        repo.registerLifecycle(new CacheManagerLifecycle(jmxService));
      }
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Lifecycle for cache manager.
   */
  static final class CacheManagerLifecycle implements Lifecycle {
    private ManagementService _jmxService;
    CacheManagerLifecycle(ManagementService jmxService) {
      _jmxService = jmxService;
    }
    @Override
    public void start() {
      try {
        _jmxService.init();
      } catch (CacheException ex) {
        if (ex.getCause() instanceof InstanceAlreadyExistsException == false) {
          throw ex;
        }
      }
    }
    @Override
    public void stop() {
      _jmxService.dispose();
      _jmxService = null;
    }
    @Override
    public boolean isRunning() {
      return _jmxService != null;
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SpringInfrastructureComponentFactory}.
   * @return the meta-bean, not null
   */
  public static SpringInfrastructureComponentFactory.Meta meta() {
    return SpringInfrastructureComponentFactory.Meta.INSTANCE;
  }
  static {
    JodaBeanUtils.registerMetaBean(SpringInfrastructureComponentFactory.Meta.INSTANCE);
  }

  @Override
  public SpringInfrastructureComponentFactory.Meta metaBean() {
    return SpringInfrastructureComponentFactory.Meta.INSTANCE;
  }

  @Override
  protected Object propertyGet(String propertyName, boolean quiet) {
    return super.propertyGet(propertyName, quiet);
  }

  @Override
  protected void propertySet(String propertyName, Object newValue, boolean quiet) {
    super.propertySet(propertyName, newValue, quiet);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      return super.equals(obj);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    return hash ^ super.hashCode();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SpringInfrastructureComponentFactory}.
   */
  public static class Meta extends AbstractSpringComponentFactory.Meta {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> _metaPropertyMap$ = new DirectMetaPropertyMap(
      this, (DirectMetaPropertyMap) super.metaPropertyMap());

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    public BeanBuilder<? extends SpringInfrastructureComponentFactory> builder() {
      return new DirectBeanBuilder<SpringInfrastructureComponentFactory>(new SpringInfrastructureComponentFactory());
    }

    @Override
    public Class<? extends SpringInfrastructureComponentFactory> beanType() {
      return SpringInfrastructureComponentFactory.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return _metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
