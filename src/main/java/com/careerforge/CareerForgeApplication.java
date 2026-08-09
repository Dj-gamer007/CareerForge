package com.careerforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.careerforge.config")
public class CareerForgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareerForgeApplication.class, args);
    }
}
