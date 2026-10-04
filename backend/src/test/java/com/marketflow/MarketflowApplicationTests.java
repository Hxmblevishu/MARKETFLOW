package com.marketflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MarketflowApplicationTests {

    @Test
    @DisplayName("Context loads successfully using in-memory H2 database")
    void contextLoads() {
        // Confirms Spring Boot context boots up cleanly with in-memory H2
    }
}
