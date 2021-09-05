package com.gmail.tracebachi.deltaredis.nukkit.factory;

import cn.nukkit.utils.Config;
import com.gmail.tracebachi.deltaredis.shared.redis.RedisCredentials;
import lombok.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class RedisCredentialsFactory {

    public static RedisCredentials createRedisCredentials(Config config) {
        @Nullable String password = config.getString("redis.password", null);
        @NonNull String host = config.getString("redis.host");
        int port = config.getInt("redis.port", 6379);

        if (null != password && password.equals("null")) {
            password = null;
        }

        return new RedisCredentials(password, host, port);
    }
}
