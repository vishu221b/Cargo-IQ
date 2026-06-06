package io.cargoiq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point.
 *
 * <p>Deliberately empty — all wiring lives under {@code io.cargoiq.config}.
 * Keeping this class boring is a clean-architecture signal: the framework
 * bootstrapper should not be where business decisions live.
 */
@SpringBootApplication
public class CargoIqApplication {

    public static void main(String[] args) {
        SpringApplication.run(CargoIqApplication.class, args);
    }
}
