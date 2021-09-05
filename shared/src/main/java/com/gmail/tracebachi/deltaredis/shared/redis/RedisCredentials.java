package com.gmail.tracebachi.deltaredis.shared.redis;

import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public class RedisCredentials {
    public String password;
    public @NonNull String host;
    public int port;

    public String buildRedisUri() {
        StringBuilder builder = new StringBuilder();
        builder.append("redis://");

        if (null != password) {
            builder.append(password).append('@');
        }

        return builder.append(':').append(port).toString();
    }
}
