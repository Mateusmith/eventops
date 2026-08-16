package com.eventops;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ArchitectureTest {

    @Test
    void deveRespeitarFronteirasDosModulos() {
        ApplicationModules.of(EventOpsApplication.class).verify();
    }
}
