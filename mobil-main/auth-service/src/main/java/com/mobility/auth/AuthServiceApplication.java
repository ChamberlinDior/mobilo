package com.mobility.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Point d’entrée de l’application.
 * ----------------------------------------------------------------------------
 * - scanBasePackages   ➜ détecte tous les @Service / @Controller, etc.
 * - @EntityScan        ➜ détecte les entités JPA (Ride, Driver, …) du domaine ride
 * - @EnableJpaRepositories ➜ détecte les interfaces JpaRepository correspondantes
 * - @EnableScheduling  ➜ expose le TaskScheduler utilisé par CancellationPenaltyService
 */
@SpringBootApplication(
		scanBasePackages = {
				"com.mobility.auth",
				"com.mobility.ride"         // sous-module « ride »
		}
)
@EntityScan({
		"com.mobility.auth.model",
		"com.mobility.ride.model"
})
@EnableJpaRepositories({
		"com.mobility.auth.repository",
		"com.mobility.ride.repository"
})
@EnableScheduling
public class AuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}
}
