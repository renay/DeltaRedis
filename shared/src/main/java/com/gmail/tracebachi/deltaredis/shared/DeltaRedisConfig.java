package com.gmail.tracebachi.deltaredis.shared;

import com.gmail.tracebachi.deltaredis.shared.redis.RedisCredentials;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DeltaRedisConfig {
    public final int updatePeriod;
    public final String proxyName;
    public final String serverName;
    public boolean debugEnabled;
    public final RedisCredentials credentials;
}
