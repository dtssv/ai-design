package com.aicopilot.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@MapperScan("com.aicopilot.api.mapper")
public class AiCopilotApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCopilotApiApplication.class, args);
    }
}