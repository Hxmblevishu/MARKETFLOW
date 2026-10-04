package com.marketflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MarketflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketflowApplication.class, args);
    }
}
