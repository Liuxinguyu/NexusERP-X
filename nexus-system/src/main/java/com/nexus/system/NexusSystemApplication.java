package com.nexus.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@MapperScan("com.nexus.system.infrastructure.mapper")
public class NexusSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(NexusSystemApplication.class, args);
    }
}

