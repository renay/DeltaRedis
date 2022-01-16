package com.gmail.tracebachi.deltaredis.shared.redis;

import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public class RedisCredentials {
    public String password;
    public @NonNull String host;
    public int port;
}
