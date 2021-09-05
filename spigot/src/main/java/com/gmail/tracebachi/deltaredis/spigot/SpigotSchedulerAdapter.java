package com.gmail.tracebachi.deltaredis.spigot;

import me.loper.scheduler.AbstractJavaScheduler;
import me.loper.scheduler.SchedulerTask;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class SpigotSchedulerAdapter extends AbstractJavaScheduler {

    private final Executor executor;
    private final DeltaRedis plugin;

    public SpigotSchedulerAdapter(DeltaRedis plugin) {
        super("deltaredis-scheduler");
        this.plugin = plugin;
        this.executor = r -> plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, r);
    }

    @Override
    public Executor sync() {
        return this.executor;
    }

    @Override
    public SchedulerTask syncRepeating(Runnable runnable, long l, TimeUnit timeUnit) {
        Runnable r = () -> plugin.getServer().getScheduler()
                .runTask(plugin, runnable);

        return this.asyncRepeating(r, l, timeUnit);
    }
}
