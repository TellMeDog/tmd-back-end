package com.tmd.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TmdBackEndApplication {

    public static void main(String[] args) {
        SpringApplication.run(TmdBackEndApplication.class, args);
    }

}
