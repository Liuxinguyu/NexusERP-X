package com.nexus.wage;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({"com.nexus.wage.infrastructure.mapper", "com.nexus.oa.infrastructure.mapper"})
public class NexusWageApplication {
    public static void main(String[] args) {
        SpringApplication.run(NexusWageApplication.class, args);
    }
}
