package com.nexus.erp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.nexus.erp.infrastructure.mapper")
public class NexusErpApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusErpApplication.class, args);
    }
}

