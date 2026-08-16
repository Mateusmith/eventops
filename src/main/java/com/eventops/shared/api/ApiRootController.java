package com.eventops.shared.api;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiRootController {

    @GetMapping("/")
    Map<String, Object> raiz() {
        return Map.of(
                "aplicacao", "EventOps",
                "status", "online",
                "documentacao", "/swagger-ui.html",
                "saude", "/actuator/health");
    }
}
