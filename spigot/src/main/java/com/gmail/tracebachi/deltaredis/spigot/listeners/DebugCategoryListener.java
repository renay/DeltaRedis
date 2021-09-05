package com.gmail.tracebachi.deltaredis.spigot.listeners;

import com.gmail.tracebachi.deltaredis.shared.structure.Registerable;
import com.gmail.tracebachi.deltaredis.shared.structure.Shutdownable;
import com.gmail.tracebachi.deltaredis.spigot.DeltaRedis;
import com.gmail.tracebachi.deltaredis.spigot.events.DebugCategoryChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class DebugCategoryListener implements Listener, Registerable, Shutdownable {

    private final DeltaRedis plugin;

    public DebugCategoryListener(DeltaRedis plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDebugCategoryChange(DebugCategoryChangeEvent event) {
        String debugCategory = event.getDebugCategory();

        if (debugCategory.equalsIgnoreCase("DeltaRedis")) {
            plugin.setDebugEnabled(event.shouldEnable());
        } else if (debugCategory.equalsIgnoreCase("DeltaRedisBungee")) {
            event.setForwardToBungee(true);
        }
    }

    @Override
    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void unregister() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public void shutdown() {
    }
}
