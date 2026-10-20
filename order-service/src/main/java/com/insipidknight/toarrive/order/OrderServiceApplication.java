package com.insipidknight.toarrive.order;

import com.insipidknight.toarrive.api.client.LocationClient;
import com.insipidknight.toarrive.api.client.PaymentClient;
import com.insipidknight.toarrive.api.client.PushClient;
import com.insipidknight.toarrive.api.client.UserClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.service.registry.ImportHttpServices;

@ImportHttpServices(group = "user-service", types = UserClient.class)
@ImportHttpServices(group = "payment-service", types = PaymentClient.class)
@ImportHttpServices(group = "push-service", types = PushClient.class)
@ImportHttpServices(group = "location-service", types = LocationClient.class)
@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

}
