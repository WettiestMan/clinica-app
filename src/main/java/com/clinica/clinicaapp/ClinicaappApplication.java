package com.clinica.clinicaapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {"com.clinica.clinicaapp.entity", "com.clinica.clinicaapp.model"})
@EnableJpaRepositories(basePackages = "com.clinica.clinicaapp.repository")
public class ClinicaappApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicaappApplication.class, args);
    }
}