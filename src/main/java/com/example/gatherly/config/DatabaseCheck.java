package com.example.gatherly.config;

import java.sql.Connection;

import javax.sql.DataSource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Checks the configured database connection once when the application starts. */
@Configuration
public class DatabaseCheck {

    @Bean
    CommandLineRunner checkDatabaseConnection(DataSource dataSource) {
        return args -> {
            try (Connection connection = dataSource.getConnection()) {
                System.out.println("=====================================");
                System.out.println("DATABASE CONNECTED: " + connection.getCatalog());
                System.out.println("=====================================");
            }
        };
    }
}
