package com.hearthgames.client;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class HearthGamesClientApplication {

    public static void main(String[] args) throws Exception {
        new SpringApplicationBuilder(HearthGamesClientApplication.class)
                .headless(false)
                .web(false)
                .run(args);
    }
}