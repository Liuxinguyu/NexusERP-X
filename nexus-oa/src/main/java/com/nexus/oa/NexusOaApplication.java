package com.nexus.oa;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.nexus.oa.infrastructure.mapper")
public class NexusOaApplication {
    public static void main(String[] args) {
        SpringApplication.run(NexusOaApplication.class, args);
    }
}
