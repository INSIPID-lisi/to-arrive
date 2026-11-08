package com.insipidknight.toarrive.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderServiceApplicationTests {

    @Autowired
    private ClientHttpRequestFactory clientHttpRequestFactory;

    @Test
    void contextLoads() {
    }

    @Test
    void usesApacheHttpClient() {
        assertThat(clientHttpRequestFactory).isInstanceOf(HttpComponentsClientHttpRequestFactory.class);
    }

}
