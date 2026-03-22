package com.prepcreatine.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

/**
 * Demo mode configuration — controls the entire demo mode behaviour.
 * BSDD v2.1 §5: DemoModeConfig.java [NEW]
 *
 * Activated by: DEMO_MODE=true environment variable
 * Default:      false (production safe — demo mode must be explicitly enabled)
 */
@Configuration
public class DemoModeConfig {

    @Value("${app.demo-mode:false}")
    private boolean demoMode;

    public boolean isDemoMode() { return demoMode; }

    /** Fixed demo user UUID — used everywhere. [REQUIRED] Reference ONLY this constant. */
    public static final UUID DEMO_USER_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000001");

    /** Fixed demo conversation UUID for initial thermodynamics chat. */
    public static final UUID DEMO_CONVERSATION_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000002");
}
