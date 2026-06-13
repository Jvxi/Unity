package com.jvxi.unity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = RedisRepositoriesAutoConfiguration.class)
public class UnityApplication {
    public static void main(String[] args) {
        SpringApplication.run(UnityApplication.class, args);
    }
}
