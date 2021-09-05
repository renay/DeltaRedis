package com.gmail.tracebachi.deltaredis.bungee;

import me.loper.scheduler.AbstractJavaScheduler;
import me.loper.scheduler.SchedulerTask;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class BungeeSchedulerAdapter extends AbstractJavaScheduler {
    private final DeltaRedis plugin;
    private final Executor executor;

    public BungeeSchedulerAdapter(DeltaRedis plugin) {
        super("deltaredis-scheduler");
        this.plugin = plugin;
        this.executor = r -> this.plugin.getProxy().getScheduler().runAsync(plugin, r);
    }

    @Override
    public Executor sync() {
        return this.executor;
    }

    @Override
    public SchedulerTask syncRepeating(Runnable runnable, long l, TimeUnit timeUnit) {
        return this.asyncRepeating(() -> this.sync().execute(runnable), l, timeUnit);
    }
}
