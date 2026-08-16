package com.eventops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class EventOpsApplication {

    public static void main(String[] argumentos) {
        SpringApplication.run(EventOpsApplication.class, argumentos);
    }
}
