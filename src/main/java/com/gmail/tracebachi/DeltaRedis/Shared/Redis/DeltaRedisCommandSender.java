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
package com.gmail.tracebachi.DeltaRedis.Shared.Redis;

import com.gmail.tracebachi.DeltaRedis.Shared.Cache.CachedPlayer;
import com.gmail.tracebachi.DeltaRedis.Shared.DeltaRedisInterface;
import com.gmail.tracebachi.DeltaRedis.Shared.EscapeAndDelimiterUtil;
import com.gmail.tracebachi.DeltaRedis.Shared.Interfaces.Shutdownable;
import com.gmail.tracebachi.DeltaRedis.Shared.Servers;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import lombok.NonNull;

import java.util.*;

/**
 * Created by Trace Bachi (tracebachi@gmail.com) on 10/18/15.
 */
public class DeltaRedisCommandSender implements Shutdownable {
    private final String serverName;
    private final String bungeeName;
    private final String serverSetKey;
    private final String playerSetKey;

    private StatefulRedisConnection<String, String> connection;
    private DeltaRedisInterface plugin;
    private boolean isBungeeCordOnline;
    private Set<String> cachedServers;
    private Set<CachedPlayer> cachedPlayers;

    public DeltaRedisCommandSender(StatefulRedisConnection<String, String> connection, DeltaRedisInterface plugin) {
        this.plugin = plugin;
        this.connection = connection;
        this.bungeeName = plugin.getBungeeName();
        this.serverName = plugin.getServerName();
        this.serverSetKey = bungeeName + ":servers";
        this.playerSetKey = bungeeName + ":players";
    }

    /**
     * Sets up by adding the server to Redis.
     */
    public synchronized void setup() {
        plugin.debug("DeltaRedisCommandSender.setup()");
        connection.sync().sadd(serverSetKey, serverName);
    }

    /**
     * Shuts down by removing the server from Redis.
     */
    @Override
    public synchronized void shutdown() {
        plugin.debug("DeltaRedisCommandSender.shutdown()");

        connection.sync().srem(serverSetKey, serverName);
        connection = null;
        plugin = null;
    }

    /**
     * @return An unmodifiable set of servers that are part of the same
     * BungeeCord (from Redis)
     */
    public synchronized Set<String> getServers() {
        plugin.debug("DeltaRedisCommandSender.getServers()");

        Set<String> result = connection.sync().smembers(serverSetKey);

        isBungeeCordOnline = result.remove(Servers.BUNGEECORD);
        cachedServers = Collections.unmodifiableSet(result);
        return cachedServers;
    }

    /**
     * @return An unmodifiable set of servers that are part of the same
     * BungeeCord (from last call to {@link #getServers()})
     */
    public Set<String> getCachedServers() {
        return cachedServers;
    }

    /**
     * @return True if the BungeeCord instance was last known to be online
     * or false
     */
    public boolean isBungeeCordOnline() {
        return isBungeeCordOnline;
    }

    /**
     * @return An unmodifiable set of player names that are part of the
     * same BungeeCord (from Redis)
     */
    public synchronized Set<CachedPlayer> getPlayers() {
        plugin.debug("DeltaRedisCommandSender.getPlayers()");

        List<String> members = connection.sync().hvals(playerSetKey);
        Set<CachedPlayer> result = new HashSet<>();

        members.forEach(m -> result.add(createCachedPlayer(decompressHashMap(m))));

        cachedPlayers = Collections.unmodifiableSet(result);

        return cachedPlayers;
    }

    /**
     * @return True if the player set was removed from Redis or false
     * <p>This method does not remove individual player hashes.</p>
     */
    public synchronized boolean removePlayers() {
        plugin.debug("DeltaRedisCommandSender.removePlayers()");

        cachedPlayers = Collections.emptySet();
        return (connection.sync().del(playerSetKey)) > 0;
    }

    /**
     * @return An unmodifiable set of player names that are part of the
     * same BungeeCord (from last call to {@link #getPlayers()})
     */
    public Set<CachedPlayer> getCachedPlayers() {
        return cachedPlayers;
    }

    /**
     * Publishes a string message using Redis PubSub
     * <p>See {@link Servers} for special destination values.</p>
     *
     * @param dest         Server name that message should go to
     * @param channel      Custom channel name for the message
     * @param messageParts String message parts to send
     * @return The number of servers that received the message
     */
    public synchronized long publish(String dest, String channel, String... messageParts) {
        return publish(dest, channel, Arrays.asList(messageParts));
    }

    /**
     * Publishes a string message using Redis PubSub
     * <p>See {@link Servers} for special destination values.</p>
     *
     * @param dest         Server name that message should go to
     * @param channel      Custom channel name for the message
     * @param messageParts String message parts to send
     * @return The number of servers that received the message
     */
    public synchronized long publish(@NonNull String dest, @NonNull String channel, @NonNull List<String> messageParts) {
        plugin.debug("DeltaRedisCommandSender.publish()");

        List<String> updatedList = new ArrayList<>(messageParts.size() + 2);
        updatedList.add(serverName);
        updatedList.add(channel);

        // Add the rest of the message parts
        // Why: {dest, channel, {escaped parts}} vs. {dest, channel, part1, part2, ...}
        updatedList.addAll(messageParts);

        String escaped = EscapeAndDelimiterUtil.DELTA_SEPARATED.escapeAndDelimit(updatedList);

        plugin.debug("Sending message: " + escaped);
        return connection.sync().publish(bungeeName + ':' + dest, escaped);
    }

    /**
     * Returns a player from Redis
     *
     * @param playerName Name of the player to find
     * @return CachedPlayer if found and null if not
     */
    public synchronized CachedPlayer getPlayer(@NonNull String playerName) {
        String playerNameLower = playerName.toLowerCase();
        plugin.debug("DeltaRedisCommandSender.getPlayer(" + playerNameLower + ")");

        String serializedResult = connection.sync().hget(this.playerSetKey, playerNameLower);

        if (serializedResult == null) {
            return null;
        }

        Map<String, String> result = decompressHashMap(serializedResult);

        return createCachedPlayer(result);
    }

    private CachedPlayer createCachedPlayer(Map<String, String> result) {
        String originalName = result.get("originalName");
        String server = result.get("server");
        String ip = result.get("ip");

        if (null == server || null == ip || null == originalName) {
            return null;
        }

        return new CachedPlayer(ip, server, originalName);
    }

    /**
     * Removes a player from Redis
     *
     * @param playerName Name of the player to remove
     */
    public synchronized void removePlayer(@NonNull String playerName) {

        playerName = playerName.toLowerCase();
        plugin.debug("DeltaRedisCommandSender.removePlayer(" + playerName + ")");

        connection.sync().hdel(playerSetKey, playerName);
    }

    /**
     * Updates a player in Redis
     *
     * @param playerName Name of the player to update
     */
    public synchronized void updatePlayer(@NonNull String playerName, @NonNull Map<String, String> newValues) {

        playerName = playerName.toLowerCase();
        plugin.debug("DeltaRedisCommandSender.updatePlayer(" + playerName + ")");

        String serializedMap = compressHashMap(newValues);

        connection.sync().hset(this.playerSetKey, playerName, serializedMap);
    }

    private String compressHashMap(@NonNull Map<String, String> newValues) {
        return Joiner.on(",").withKeyValueSeparator("=").join(newValues);
    }

    private Map<String, String> decompressHashMap(String serializedResult) {
        return Splitter.on(',')
                .withKeyValueSeparator('=')
                .split(serializedResult);
    }

    private String getPlayerHashKey(String playerName) {
        return bungeeName + ":players:" + playerName;
    }
}
