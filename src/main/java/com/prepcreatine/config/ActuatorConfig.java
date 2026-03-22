package com.prepcreatine.config;

import org.springframework.context.annotation.Configuration;

/**
 * Spring Actuator configuration.
 * Exposed endpoints: health, info, metrics only (per BSDD §1).
 * All configuration declared in application.properties under management.*
 */
@Configuration
public class ActuatorConfig {
    // All Actuator configuration is in application.properties:
    // management.endpoints.web.exposure.include=health,info,metrics
    // management.endpoint.health.show-details=when-authorized
}
