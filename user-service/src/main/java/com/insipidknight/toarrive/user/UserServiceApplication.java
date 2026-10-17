package com.insipidknight.toarrive.user;

import com.insipidknight.toarrive.api.client.LocationClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.service.registry.ImportHttpServices;

@ImportHttpServices(
        group = "location",
        types = LocationClient.class
)
@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

}
