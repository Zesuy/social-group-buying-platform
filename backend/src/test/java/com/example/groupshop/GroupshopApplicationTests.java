package com.example.groupshop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Application startup test.
 * Verifies that the Spring context loads without errors.
 */
@SpringBootTest
@ActiveProfiles("test")
class GroupshopApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void healthControllerBeanExists() {
        assertThat(applicationContext.containsBean("healthController")).isTrue();
    }
}
