package com.gmail.tracebachi.deltaredis.shared.redis;

import com.gmail.tracebachi.deltaredis.shared.PluginSource;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnection;

public class RedisConnectionManager implements PluginSource {

    private StatefulRedisPubSubConnection<String, String> pubSub;
    private StatefulRedisConnection<String, String> connection;

    private final RedisClient client;

    public RedisConnectionManager(RedisClientFactory factory) {
        this.client = factory.create();
    }

    public StatefulRedisPubSubConnection<String, String> getPubSub() {
        if (null == this.pubSub) {
            this.pubSub = this.client.connectPubSub();
        }

        return this.pubSub;
    }

    public StatefulRedisConnection<String, String> getConnection() {
        if (null == this.connection) {
            this.connection = this.client.connect();
        }

        return this.connection;
    }

    @Override
    public void register() {
        this.getPubSub();
        this.getConnection();
    }

    @Override
    public void unregister() {
        this.getPubSub().close();
        this.getConnection().close();
    }

    @Override
    public void shutdown() {
        if (this.pubSub.isOpen()) {
            this.pubSub.close();
        }

        if (this.connection.isOpen()) {
            this.connection.close();
        }

        this.client.getResources().shutdown();
        this.client.shutdown();
    }
}
