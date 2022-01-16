package com.gmail.tracebachi.deltaredis.shared.redis;

import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.resource.ClientResources;
import com.lambdaworks.redis.resource.DefaultClientResources;

public class RedisClientFactory {

    private final RedisCredentials credentials;

    public RedisClientFactory(RedisCredentials credentials) {
        this.credentials = credentials;
    }

    private ClientResources createResources() {
        return DefaultClientResources.builder()
                .ioThreadPoolSize(3)
                .computationThreadPoolSize(3)
                .build();
    }

    private RedisClient createClient(ClientResources resources, RedisCredentials credentials) {
        RedisClient client = RedisClient.create(resources, createRedisUri(credentials));
        client.setOptions(ClientOptions.builder().autoReconnect(true).build());

        return client;
    }

    private RedisURI createRedisUri(RedisCredentials credentials) {
        RedisURI.Builder builder = RedisURI.Builder.redis(
                credentials.host, credentials.port);
        String password = credentials.password;

        if (null != password && 0 != password.length()) {
            builder = builder.withPassword(password);
        }

        return builder.build();
    }

    public RedisClient create() {
        ClientResources resources = createResources();
        return createClient(resources, credentials);
    }
}
