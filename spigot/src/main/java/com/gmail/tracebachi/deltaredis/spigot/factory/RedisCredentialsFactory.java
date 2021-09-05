package com.gmail.tracebachi.deltaredis.spigot.factory;

import com.gmail.tracebachi.deltaredis.shared.redis.RedisCredentials;
import org.bukkit.configuration.ConfigurationSection;

public class RedisCredentialsFactory {

    public static RedisCredentials createRedisCredentials(ConfigurationSection config) {
        String password = config.getString("redis.password", null);
        String host = config.getString("redis.host");
        int port = config.getInt("redis.port", 6379);

        if (null == host) {
            throw new IllegalStateException("Address can not be null.");
        }

        if (null != password && password.equals("null")) {
            password = null;
        }

        return new RedisCredentials(password, host, port);
    }
}
