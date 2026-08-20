package com.atguigu.yygh.appointment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {"com.atguigu"})
@EnableDiscoveryClient
@EnableScheduling
@MapperScan("com.atguigu.yygh.appointment.infrastructure.mapper")
public class AppointmentCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppointmentCoreApplication.class, args);
    }
}
