package com.example.groupshop.base;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for Service unit tests.
 * Loads full Spring context with H2 in-memory database + Flyway migration.
 * Subclasses can @Autowired service classes directly.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class ServiceTestBase {
}
