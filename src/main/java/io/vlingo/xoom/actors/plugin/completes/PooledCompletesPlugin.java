// Copyright © 2012-2022 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.actors.plugin.completes;

import java.util.Properties;

import io.vlingo.xoom.actors.CompletesEventuallyProvider;
import io.vlingo.xoom.actors.Configuration;
import io.vlingo.xoom.actors.Registrar;
import io.vlingo.xoom.actors.plugin.AbstractPlugin;
import io.vlingo.xoom.actors.plugin.Plugin;
import io.vlingo.xoom.actors.plugin.PluginConfiguration;
import io.vlingo.xoom.actors.plugin.PluginProperties;

public class PooledCompletesPlugin extends AbstractPlugin implements Plugin {
  private CompletesEventuallyProvider completesEventuallyProvider;
  private final PooledCompletesPluginConfiguration pooledCompletesPluginConfiguration;

  public PooledCompletesPlugin() {
    this.pooledCompletesPluginConfiguration = PooledCompletesPluginConfiguration.define();
  }

  @Override
  public void close() {
    completesEventuallyProvider.close();
  }

  @Override
  public PluginConfiguration configuration() {
    return pooledCompletesPluginConfiguration;
  }

  @Override
  public String name() {
    return pooledCompletesPluginConfiguration.name();
  }

  @Override
  public int pass() {
    return 2;
  }

  @Override
  public void start(final Registrar registrar) {
    this.completesEventuallyProvider = new CompletesEventuallyPool(this.pooledCompletesPluginConfiguration.poolSize, this.pooledCompletesPluginConfiguration.mailbox);
    registrar.register(pooledCompletesPluginConfiguration.name(), completesEventuallyProvider);
  }

  @Override
  public Plugin with(final PluginConfiguration overrideConfiguration) {
    if (overrideConfiguration == null) {
      return this;
    }
    return new PooledCompletesPlugin(overrideConfiguration);
  }

  @Override
  public void __internal_Only_Init(final String name, final Configuration configuration, final Properties properties) {
    this.pooledCompletesPluginConfiguration.name = name;
  }

  private PooledCompletesPlugin(final PluginConfiguration configuration) {
    this.pooledCompletesPluginConfiguration = (PooledCompletesPluginConfiguration) configuration;
  }

  public static class PooledCompletesPluginConfiguration implements PluginConfiguration {
    private String mailbox;
    private String name = "pooledCompletes";
    private int poolSize;

    public static PooledCompletesPluginConfiguration define() {
      return new PooledCompletesPluginConfiguration();
    }

    public PooledCompletesPluginConfiguration mailbox(final String mailbox) {
      this.mailbox = mailbox;
      return this;
    }

    public String mailbox() {
      return mailbox;
    }

    public PooledCompletesPluginConfiguration poolSize(final int poolSize) {
      this.poolSize = poolSize;
      return this;
    }

    public int poolSize() {
      return poolSize;
    }

    @Override
    public void build(final Configuration configuration) {
      configuration.with(mailbox("queueMailbox").poolSize(10));
    }

    @Override
    public void buildWith(final Configuration configuration, final PluginProperties properties) {
      this.name = properties.name;
      this.poolSize = properties.getInteger("pool", 10);
      this.mailbox = properties.getString("mailbox", null);
      configuration.with(this);
    }

    @Override
    public String name() {
      return name;
    }
  }
}
