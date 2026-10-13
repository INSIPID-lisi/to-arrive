package com.insipidknight.toarrive;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.insipidknight.toarrive.**.mapper")
public class ToArriveApplication {

    public static void main(String[] args) {
        SpringApplication.run(ToArriveApplication.class, args);
    }

}
