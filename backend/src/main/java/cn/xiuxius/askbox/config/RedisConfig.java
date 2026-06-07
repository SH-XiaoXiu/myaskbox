package cn.xiuxius.askbox.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.util.StringUtils;

@Configuration
public class RedisConfig {

    @Bean
    public RedisStandaloneConfiguration redisStandaloneConfiguration(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port,
            @Value("${spring.data.redis.database:0}") int database,
            @Value("${spring.data.redis.username:}") String username,
            @Value("${spring.data.redis.password:}") String password) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        config.setDatabase(database);
        if (StringUtils.hasText(username)) {
            config.setUsername(username);
        }
        if (StringUtils.hasText(password)) {
            config.setPassword(RedisPassword.of(password));
        }
        return config;
    }
}
