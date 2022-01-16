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
package com.gmail.tracebachi.deltaredis.bungee;

import com.gmail.tracebachi.deltaredis.bungee.commands.DebugCategoryCommand;
import com.gmail.tracebachi.deltaredis.bungee.commands.RunCmdCommand;
import com.gmail.tracebachi.deltaredis.bungee.events.DeltaRedisMessageEvent;
import com.gmail.tracebachi.deltaredis.bungee.factory.RedisCredentialsFactory;
import com.gmail.tracebachi.deltaredis.bungee.listeners.ProxiedPlayerListener;
import com.gmail.tracebachi.deltaredis.bungee.utils.ConfigUtil;
import com.gmail.tracebachi.deltaredis.shared.ChatMessageHelper;
import com.gmail.tracebachi.deltaredis.shared.DeltaRedisApi;
import com.gmail.tracebachi.deltaredis.shared.DeltaRedisConfig;
import com.gmail.tracebachi.deltaredis.shared.PluginSource;
import com.gmail.tracebachi.deltaredis.shared.redis.*;
import com.gmail.tracebachi.deltaredis.shared.structure.Channel;
import com.gmail.tracebachi.deltaredis.shared.structure.Composer;
import com.gmail.tracebachi.deltaredis.shared.structure.DeltaRedisPlugin;
import com.google.common.base.Preconditions;
import me.loper.scheduler.SchedulerAdapter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by Trace Bachi (tracebachi@gmail.com) on 10/18/15.
 */
public class DeltaRedis extends Plugin implements DeltaRedisPlugin {

    private final Composer<PluginSource> sources = new Composer<>();

    private BungeeSchedulerAdapter scheduler;
    private RedisConnectionManager manager;
    private DeltaRedisConfig config;

    @Override
    public void onEnable() {
        info("-----------------------------------------------------------------");
        info("[IMPORTANT] Please verify that all Spigot servers are configured");
        info("[IMPORTANT] with their correct cased name. For example: ");
        info("[IMPORTANT] 'World' is not the same as 'world'");
        for (Map.Entry<String, ServerInfo> entry : getProxy().getServers().entrySet()) {
            info("[IMPORTANT] Case-sensitive server name: " + entry.getValue().getName());
        }
        info("-----------------------------------------------------------------");

        this.scheduler = new BungeeSchedulerAdapter(this);
        this.config = readConfig(loadConfig());

        RedisClientFactory factory = new RedisClientFactory(this.config.credentials);
        this.manager = new RedisConnectionManager(factory);

        DeltaRedisCommandSender commandSender = new DeltaRedisCommandSender(manager.getConnection(), this);

        this.sources.add(new DeltaRedisPubSubListener(this));
        this.sources.add(commandSender);
        this.sources.add(this.manager);

        this.sources.add(new DebugCategoryCommand(this));
        this.sources.add(new RunCmdCommand(this));
        this.sources.add(new ProxiedPlayerListener(commandSender, this));

        this.sources.register();

        DeltaRedisApi.setup(commandSender, this);

        getProxy().getScheduler().schedule(this, () ->
        {
            commandSender.getServers();
            commandSender.getPlayers();
        }, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public void onDisable() {
        getProxy().getScheduler().cancel(this);

        DeltaRedisApi.shutdown();

        this.sources.unregister();
        this.sources.shutdown();
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.config.debugEnabled = debugEnabled;
    }

    @Override
    public void onRedisMessageEvent(List<String> allMessageParts) {
        Preconditions.checkNotNull(allMessageParts, "allMessageParts");
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
    public void onRedisMessageEvent(String sendingServer, String channel, List<String> messageParts) {
        Preconditions.checkNotNull(sendingServer, "sendingServer");
        Preconditions.checkNotNull(channel, "channel");
        Preconditions.checkNotNull(messageParts, "messageParts");

        DeltaRedisMessageEvent event = new DeltaRedisMessageEvent(
                sendingServer,
                channel,
                messageParts);

        getProxy().getPluginManager().callEvent(event);
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
        if (this.config.debugEnabled) {
            getLogger().info("[Debug] " + message);
        }
    }

    @Override
    public SchedulerAdapter getScheduler() {
        return this.scheduler;
    }

    @Override
    public void sendConsoleCommand(String command) {
        this.getProxy().getPluginManager().dispatchCommand(this.getProxy().getConsole(), command);
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

    private Configuration loadConfig() {
        try {
            File file = ConfigUtil.saveResource(
                    this,
                    "config.yml",
                    "config.yml");
            Configuration config = ConfigurationProvider
                    .getProvider(YamlConfiguration.class)
                    .load(file);

            if (config != null) {
                return config;
            }

            ConfigUtil.saveResource(
                    this,
                    "config.yml",
                    "config-example.yml",
                    true);

            throw new RuntimeException("Failed to load configuration file");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration file", e);
        }
    }

    private DeltaRedisConfig readConfig(Configuration configuration) {
        Preconditions.checkNotNull(configuration, "configuration");

        String proxyName = configuration.getString("proxy-name");
        boolean debugEnabled = configuration.getBoolean("debug");

        Preconditions.checkNotNull(proxyName, "proxyName");

        Configuration formatsSection = configuration.getSection("formats");

        if (formatsSection == null) {
            throw new IllegalStateException("Can not read format section.");
        }

        for (String key : formatsSection.getKeys()) {
            String value = formatsSection.getString(key);
            if (value != null) {
                String translatedFormat = ChatColor
                        .translateAlternateColorCodes('&', value);
                ChatMessageHelper.instance().updateFormat("deltaredisbungee." + key, translatedFormat);
            }
        }

        RedisCredentials credentials = RedisCredentialsFactory.createRedisCredentials(configuration);

        return new DeltaRedisConfig(0, proxyName, proxyName, debugEnabled, credentials);
    }
}
