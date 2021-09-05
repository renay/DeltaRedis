/*
 * This file is part of DeltaRedis.
 *
 * DeltaRedis is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DeltaRedis is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DeltaRedis.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.gmail.tracebachi.deltaredis.spigot;

import com.gmail.tracebachi.deltaredis.shared.ChatMessageHelper;
import com.gmail.tracebachi.deltaredis.shared.DeltaRedisApi;
import com.gmail.tracebachi.deltaredis.shared.DeltaRedisConfig;
import com.gmail.tracebachi.deltaredis.shared.PluginSource;
import com.gmail.tracebachi.deltaredis.shared.redis.*;
import com.gmail.tracebachi.deltaredis.shared.structure.Channel;
import com.gmail.tracebachi.deltaredis.shared.structure.Composer;
import com.gmail.tracebachi.deltaredis.shared.structure.DeltaRedisPlugin;
import com.gmail.tracebachi.deltaredis.spigot.commands.DebugCategoryCommand;
import com.gmail.tracebachi.deltaredis.spigot.commands.IsOnlineCommand;
import com.gmail.tracebachi.deltaredis.spigot.commands.RunCmdCommand;
import com.gmail.tracebachi.deltaredis.spigot.events.DeltaRedisMessageEvent;
import com.gmail.tracebachi.deltaredis.spigot.factory.RedisCredentialsFactory;
import com.gmail.tracebachi.deltaredis.spigot.listeners.DeltaRedisChatMessageListener;
import com.google.common.base.Preconditions;
import lombok.NonNull;
import me.loper.scheduler.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Trace Bachi (tracebachi@gmail.com) on 10/18/15.
 */
public class DeltaRedis extends JavaPlugin implements DeltaRedisPlugin {

    private final Composer<PluginSource> sources = new Composer<>();

    private RedisConnectionManager manager;
    private SchedulerAdapter scheduler;
    private DeltaRedisConfig config;

    @Override
    public void onLoad() {
        saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        info("-----------------------------------------------------------------");
        info("[IMPORTANT] Please make sure that 'ServerName' is *exactly* the");
        info("[IMPORTANT] same as your BungeeCord config for this server.");
        info("[IMPORTANT] DeltaRedis and all plugins that depend on it may not");
        info("[IMPORTANT] run correctly if the name is not correct.");
        info("[IMPORTANT] 'World' is not the same as 'world'");
        info("-----------------------------------------------------------------");

        reloadConfig();

        this.scheduler = new SpigotSchedulerAdapter(this);
        this.config = readConfig(getConfig());

        RedisClientFactory factory = new RedisClientFactory(this.config.credentials);
        this.manager = new RedisConnectionManager(factory);

        DeltaRedisCommandSender commandSender = new DeltaRedisCommandSender(this.manager.getConnection(), this);

        this.sources.add(new DeltaRedisPubSubListener(this));
        this.sources.add(commandSender);
        this.sources.add(this.manager);

        this.sources.add(new DeltaRedisChatMessageListener(this));
        this.sources.add(new DebugCategoryCommand(this));
        this.sources.add(new IsOnlineCommand(this));
        this.sources.add(new RunCmdCommand(this));

        this.sources.register();

        DeltaRedisApi.setup(commandSender, this);

        scheduler.asyncRepeating(() -> {
            commandSender.getServers();
            commandSender.getPlayers();
        }, 1, TimeUnit.SECONDS);
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        this.scheduler.shutdownExecutor();
        this.scheduler.shutdownScheduler();

        DeltaRedisApi.shutdown();
        this.sources.unregister();
        this.sources.shutdown();
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.config.debugEnabled = debugEnabled;
    }

    @Override
    public void onRedisMessageEvent(@NonNull List<String> allMessageParts) {
        Preconditions.checkArgument(
                allMessageParts.size() >= 2,
                "Less than expected number of parts in message");

        String sendingServer = allMessageParts.get(0);
        String channel = allMessageParts.get(1);
        List<String> eventMessageParts = new ArrayList<>(allMessageParts.size() - 2);

        for (int i = 2; i < allMessageParts.size(); i++) {
            eventMessageParts.add(allMessageParts.get(i));
        }

        onRedisMessageEvent(sendingServer, channel, eventMessageParts);
    }

    @Override
    public void onRedisMessageEvent(@NonNull String sendingServer, @NonNull String channel, @NonNull List<String> messageParts) {

        if (getServerName().equals(sendingServer)) {
            return;
        }

        DeltaRedisMessageEvent event = new DeltaRedisMessageEvent(
                sendingServer,
                channel,
                messageParts);

        Bukkit.getPluginManager().callEvent(event);
    }

    @Override
    public String getProxyName() {
        return this.config.proxyName;
    }

    @Override
    public String getServerName() {
        return this.config.serverName;
    }

    @Override
    public void info(String message) {
        getLogger().info(message);
    }

    @Override
    public void severe(String message) {
        getLogger().severe(message);
    }

    @Override
    public void debug(String message) {
        if (config.debugEnabled) {
            getLogger().info("[Debug] " + message);
        }
    }

    @Override
    public SchedulerAdapter getScheduler() {
        return this.scheduler;
    }

    @Override
    public void sendConsoleCommand(String command) {
        this.getServer().dispatchCommand(this.getServer().getConsoleSender(), command);
    }

    @Override
    public DeltaRedisApi getApi() {
        return DeltaRedisApi.instance();
    }

    @Override
    public RedisConnectionManager getRedisConnectionManager() {
        return this.manager;
    }

    @Override
    public String getCommonChannel() {
        return Channel.PROXY;
    }

    private @NonNull DeltaRedisConfig readConfig(ConfigurationSection configuration) {
        Preconditions.checkNotNull(configuration, "configuration");

        boolean debugEnabled = configuration.getBoolean("debug", false);
        String proxyName = configuration.getString("proxy-name");
        String serverName = configuration.getString("server-name");
        int updatePeriod = getConfig().getInt("online-update-period", 300);

        Preconditions.checkNotNull(proxyName, "proxyName");
        Preconditions.checkNotNull(serverName, "serverName");
        Preconditions.checkArgument(updatePeriod > 100, "Update period can not be less than 100ms.");

        ConfigurationSection formatsSection = configuration.getConfigurationSection("formats");

        if (formatsSection == null) {
            throw new IllegalStateException("Can not read format section.");
        }

        for (String key : formatsSection.getKeys(false)) {
            String value = formatsSection.getString(key);
            if (value != null) {
                String translatedFormat = ChatColor.translateAlternateColorCodes('&', value);
                ChatMessageHelper.instance().updateFormat("deltaredis." + key, translatedFormat);
            }
        }

        RedisCredentials credentials = RedisCredentialsFactory.createRedisCredentials(configuration);

        return new DeltaRedisConfig(updatePeriod, proxyName, serverName, debugEnabled, credentials);
    }
}
