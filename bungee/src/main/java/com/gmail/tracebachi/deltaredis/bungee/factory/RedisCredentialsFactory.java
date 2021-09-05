package com.gmail.tracebachi.deltaredis.bungee.factory;

import com.gmail.tracebachi.deltaredis.shared.redis.RedisCredentials;
import net.md_5.bungee.config.Configuration;

public class RedisCredentialsFactory {

    public static RedisCredentials createRedisCredentials(Configuration config) {
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
