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
package com.gmail.tracebachi.deltaredis.shared;

import com.gmail.tracebachi.deltaredis.shared.cache.CachedPlayer;
import com.gmail.tracebachi.deltaredis.shared.cache.CachedPlayerCallback;
import com.gmail.tracebachi.deltaredis.shared.redis.DeltaRedisCommandSender;
import com.gmail.tracebachi.deltaredis.shared.structure.DeltaRedisPlugin;
import com.gmail.tracebachi.deltaredis.shared.structure.Channel;
import com.google.common.base.Preconditions;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.gmail.tracebachi.deltaredis.shared.DeltaRedisChannels.*;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/11/15.
 */
public class DeltaRedisApi {
    private static DeltaRedisApi instance;

    private DeltaRedisCommandSender deltaSender;
    private DeltaRedisPlugin plugin;

    /**
     * @return Singleton instance of DeltaRedisApi
     */
    public static DeltaRedisApi instance() {
        return instance;
    }

    /**
     * @return Name of the BungeeCord instance to which the server belongs
     * <p>This value is set in the configuration file for each server</p>
     */
    public String getBungeeName() {
        return plugin.getProxyName();
    }

    /**
     * @return Name of the current server
     * <p>This value is set in the configuration file for each server</p>
     */
    public String getServerName() {
        return plugin.getServerName();
    }

    /**
     * @return An unmodifiable set of servers that are part of the same
     * BungeeCord (from last call to {@link DeltaRedisCommandSender#getServers()})
     */
    public Set<String> getCachedServers() {
        return deltaSender.getCachedServers();
    }

    /**
     * @return True if the BungeeCord instance was last known to be online
     * or false
     */
    public boolean isBungeeCordOnline() {
        return deltaSender.isBungeeCordOnline();
    }

    /**
     * @return An unmodifiable set of player names that are part of the
     * same BungeeCord (from last call to {@link DeltaRedisCommandSender#getPlayers()})
     */
    public Set<CachedPlayer> getCachedPlayers() {
        return deltaSender.getCachedPlayers();
    }

    /**
     * @param partial Non-null string that is the beginning of a name
     * @return A list of player names that begins with the partial
     * sent to this method
     */
    public List<String> matchStartOfPlayerName(@NonNull String partial) {
        List<String> result = new ArrayList<>();
        partial = partial.toLowerCase();

        for (CachedPlayer player : getCachedPlayers()) {
            if (player.getName().startsWith(partial)) {
                result.add(player.getOriginalName());
            }
        }

        return result;
    }

    /**
     * @param partial Non-null string that is the beginning of a name
     * @return A list of server names that begins with the partial
     * sent to this method
     */
    public List<String> matchStartOfServerName(@NonNull String partial) {
        List<String> result = new ArrayList<>();
        partial = partial.toLowerCase();

        for (String name : getCachedServers()) {
            if (name.toLowerCase().startsWith(partial)) {
                result.add(name);
            }
        }

        return result;
    }

    /**
     * Asynchronously looks for the player in Redis
     * <p>The callback is called synchronously with the {@link CachedPlayer} or null</p>
     *
     * @param playerName Name of the player to find
     * @param callback   Callback to run when fetch is complete
     */
    public void findPlayer(String playerName, CachedPlayerCallback callback) {
        findPlayer(playerName, callback, true);
    }

    /**
     * Asynchronously looks for the player in Redis
     * <p>The callback is called synchronously with the {@link CachedPlayer} or null</p>
     *
     * @param playerName   Name of the player to find
     * @param callback     Callback to run when fetch is complete
     * @param syncCallback Set to true to run callback sync else it will run async
     */
    public void findPlayer(@NonNull String playerName, @NonNull CachedPlayerCallback callback, boolean syncCallback) {
        plugin.getScheduler().executeAsync(() -> {
            CachedPlayer cachedPlayer = deltaSender.getPlayer(playerName);

            if (syncCallback) {
                plugin.getScheduler().executeSync(() -> callback.call(cachedPlayer));
            } else {
                callback.call(cachedPlayer);
            }
        });
    }

    /**
     * Publishes a message built from string message parts
     *
     * @param destination   Server to send message to
     * @param channel       Channel of the message
     * @param messagePieces The parts of the message
     */
    public void publish(String destination, String channel, String... messagePieces) {
        publish(destination, channel, Arrays.asList(messagePieces));
    }

    /**
     * Publishes a message to Redis
     *
     * @param destination  Server to send message to
     * @param channel      Channel of the message
     * @param messageParts The actual message
     */
    public void publish(@NonNull String destination, @NonNull String channel, @NonNull List<String> messageParts) {

        if (plugin.getServerName().equals(destination)) {
            plugin.getScheduler().executeSync(() -> plugin.onRedisMessageEvent(destination, channel, messageParts));
            return;
        }

        plugin.getScheduler().executeSync(() -> deltaSender.publish(destination, channel, messageParts));
    }

    /**
     * Publishes a message to Redis for all subscribed spigot servers.
     *
     * @param channel      Channel of the message
     * @param messageParts The actual message
     */
    public void publish(@NonNull String channel, @NonNull List<String> messageParts) {
        plugin.getScheduler().executeAsync(() -> deltaSender.publish(Channel.SPIGOT, channel, messageParts));
    }

    /**
     * Sends a command that will run as OP by the receiving server
     *
     * @param destServer Destination server name, {@link Channel#SPIGOT},
     *                   or {@link Channel#PROXY}
     * @param command    Command to send
     * @param sender     Name to record in the logs as having run the command
     */
    public void sendServerCommand(@NonNull String destServer, @NonNull String command, @NonNull String sender) {
        if (plugin.getServerName().equals(destServer)) {
            plugin.getScheduler().executeSync(() -> plugin.sendConsoleCommand(command));
            return;
        }

        plugin.getScheduler().executeAsync(() -> deltaSender.publish(destServer, RUN_CMD, sender, command));
    }

    /**
     * This method sends a message to a player on an unknown server. The
     * message will not reach the player if they have logged off by the
     * time the message reaches the server or if no player is online
     * by the specified name.
     *
     * @param playerName Name of the player to send message to
     * @param message    Message to send
     */
    public void sendMessageToPlayer(String playerName, String message) {
        Preconditions.checkNotNull(playerName, "playerName");
        Preconditions.checkNotNull(message, "message");

        plugin.getScheduler().executeAsync(() -> {
            CachedPlayer cachedPlayer = deltaSender.getPlayer(playerName);

            if (cachedPlayer == null) {
                return;
            }

            deltaSender.publish(cachedPlayer.getServer(), SEND_MESSAGE, playerName, message);
        });
    }

    /**
     * This method sends a message to a player on an unknown server. The
     * message will not reach the player if they have logged off by the
     * time the message reaches the server or if no player is online
     * by the specified name.
     *
     * @param server     Name of the server to send message to
     * @param playerName Name of the player to send message to
     * @param message    Message to send
     */
    public void sendMessageToPlayer(String server, String playerName, String message) {
        Preconditions.checkNotNull(playerName, "playerName");
        Preconditions.checkNotNull(message, "message");
        Preconditions.checkArgument(!server.equals(Channel.PROXY), "Message must be sent from PROXY");

        plugin.getScheduler().executeAsync(() -> deltaSender.publish(server, SEND_MESSAGE, playerName, message));
    }

    /**
     * Sends an announcement to all players on a server
     *
     * @param destServer   Destination server name or {@link Channel#SPIGOT}
     * @param announcement Announcement to send
     */
    public void sendServerAnnouncement(String destServer, String announcement) {
        sendServerAnnouncement(destServer, announcement, "");
    }

    /**
     * Sends an announcement to all players on a server with a specific
     * permission
     *
     * @param destServer   Destination server name or {@link Channel#SPIGOT}
     * @param announcement Announcement to send
     * @param permission   Permission required by players to view announcement or ""
     */
    public void sendServerAnnouncement(String destServer, String announcement, String permission) {
        Preconditions.checkNotNull(destServer, "destServer");
        Preconditions.checkNotNull(announcement, "announcement");
        Preconditions.checkNotNull(permission, "permission");

        plugin.getScheduler().executeAsync(() -> deltaSender.publish(
                destServer, SEND_ANNOUNCEMENT, permission, announcement));
    }

    /**
     * Private constructor
     */
    private DeltaRedisApi(DeltaRedisCommandSender deltaSender, DeltaRedisPlugin plugin) {
        this.deltaSender = deltaSender;
        this.plugin = plugin;
    }

    /**
     * Sets up the api instance
     */
    public static void setup(DeltaRedisCommandSender deltaSender, DeltaRedisPlugin plugin) {
        if (instance != null) {
            shutdown();
        }

        instance = new DeltaRedisApi(deltaSender, plugin);
    }

    /**
     * Cleans up the api instance
     */
    public static void shutdown() {
        if (instance != null) {
            instance.deltaSender = null;
            instance.plugin = null;
            instance = null;
        }
    }
}
