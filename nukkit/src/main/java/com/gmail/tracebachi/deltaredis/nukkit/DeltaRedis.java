package com.gmail.tracebachi.deltaredis.nukkit;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import cn.nukkit.utils.TextFormat;
import com.gmail.tracebachi.deltaredis.nukkit.event.DeltaRedisMessageEvent;
import com.gmail.tracebachi.deltaredis.nukkit.factory.RedisCredentialsFactory;
import com.gmail.tracebachi.deltaredis.shared.ChatMessageHelper;
import com.gmail.tracebachi.deltaredis.shared.DeltaRedisApi;
import com.gmail.tracebachi.deltaredis.shared.DeltaRedisConfig;
import com.gmail.tracebachi.deltaredis.shared.PluginSource;
import com.gmail.tracebachi.deltaredis.shared.redis.*;
import com.gmail.tracebachi.deltaredis.shared.structure.Composer;
import com.gmail.tracebachi.deltaredis.shared.structure.DeltaRedisPlugin;
import com.gmail.tracebachi.deltaredis.shared.structure.Channel;
import com.google.common.base.Preconditions;
import lombok.NonNull;
import me.loper.scheduler.SchedulerAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DeltaRedis extends PluginBase implements DeltaRedisPlugin {

    private final Composer<PluginSource> sources = new Composer<>();

    private NukkitSchedulerAdapter scheduler;
    private DeltaRedisConfig config;

    private RedisConnectionManager manager;

    @Override
    public void onEnable() {
        info("-----------------------------------------------------------------");
        info("[IMPORTANT] Please make sure that 'ServerName' is *exactly* the");
        info("[IMPORTANT] same as your Proxy config for this server.");
        info("[IMPORTANT] DeltaRedis and all plugins that depend on it may not");
        info("[IMPORTANT] run correctly if the name is not correct.");
        info("[IMPORTANT] 'World' is not the same as 'world'");
        info("-----------------------------------------------------------------");

        reloadConfig();

        this.scheduler = new NukkitSchedulerAdapter(this);
        this.config = readConfig(getConfig());

        RedisClientFactory factory = new RedisClientFactory(this.config.credentials);
        this.manager = new RedisConnectionManager(factory);

        DeltaRedisCommandSender commandSender = new DeltaRedisCommandSender(manager.getConnection(), this);

        this.sources.add(new DeltaRedisPubSubListener(this));
        this.sources.add(commandSender);
        this.sources.add(this.manager);

        this.sources.register();

        DeltaRedisApi.setup(commandSender, this);

        scheduler.asyncRepeating(() -> {
            commandSender.getServers();
            commandSender.getPlayers();
        }, 1, TimeUnit.SECONDS);
    }

    private @NonNull DeltaRedisConfig readConfig(Config configuration) {
        Preconditions.checkNotNull(configuration, "configuration");

        boolean debugEnabled = configuration.getBoolean("debug", false);
        String proxyName = configuration.getString("proxy-name");
        String serverName = configuration.getString("server-name");
        int updatePeriod = getConfig().getInt("online-update-period", 300);

        Preconditions.checkNotNull(proxyName, "proxyName");
        Preconditions.checkNotNull(serverName, "serverName");
        Preconditions.checkArgument(updatePeriod > 100, "Update period can not be less than 100ms.");

        ConfigSection formatsSection = configuration.getSection("formats");

        if (formatsSection == null) {
            throw new IllegalStateException("Can not read format section.");
        }

        for (String key : formatsSection.getKeys(false)) {
            String value = formatsSection.getString(key);
            if (value != null) {
                String translatedFormat = TextFormat.colorize('&', value);
                ChatMessageHelper.instance().updateFormat("deltaredis." + key, translatedFormat);
            }
        }

        RedisCredentials credentials = RedisCredentialsFactory.createRedisCredentials(configuration);

        return new DeltaRedisConfig(updatePeriod, proxyName, serverName, debugEnabled, credentials);
    }

    @Override
    public void onDisable() {
        this.scheduler.shutdownExecutor();
        this.scheduler.shutdownScheduler();

        this.sources.unregister();
        this.sources.shutdown();
    }

    @Override
    public void onRedisMessageEvent(List<String> publishedMessageParts) {
        Preconditions.checkArgument(
                publishedMessageParts.size() >= 2,
                "Less than expected number of parts in message");

        String sendingServer = publishedMessageParts.get(0);
        String channel = publishedMessageParts.get(1);
        List<String> eventMessageParts = new ArrayList<>(publishedMessageParts.size() - 2);

        for (int i = 2; i < publishedMessageParts.size(); i++) {
            eventMessageParts.add(publishedMessageParts.get(i));
        }

        onRedisMessageEvent(sendingServer, channel, eventMessageParts);
    }

    @Override
    public void onRedisMessageEvent(String server, String channel, List<String> messageParts) {

        if (getServerName().equals(server)) {
            return;
        }

        DeltaRedisMessageEvent event = new DeltaRedisMessageEvent(
                server,
                channel,
                messageParts);

        this.getServer().getPluginManager().callEvent(event);
    }

    @Override
    public String getProxyName() {
        return null;
    }

    @Override
    public String getServerName() {
        return null;
    }

    @Override
    public void info(String message) {
        getLogger().info(message);
    }

    @Override
    public void severe(String message) {
        getLogger().emergency(message);
    }

    @Override
    public void debug(String message) {
        getLogger().debug(message);
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
        return Channel.NUKKIT;
    }
}
