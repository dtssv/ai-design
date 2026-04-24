package com.aicopilot.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.aicopilot.admin.mapper")
public class AiCopilotAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCopilotAdminApplication.class, args);
    }
}