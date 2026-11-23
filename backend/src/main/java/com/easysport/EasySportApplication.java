package com.easysport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EasySportApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasySportApplication.class, args);
        System.out.println("==================================================");
        System.out.println(" EasySport Spring Boot Backend đã khởi động thành công!");
        System.out.println(" URL API: http://localhost:8080/api");
        System.out.println(" Sân bóng: http://localhost:8080/api/courts");
        System.out.println(" Đặt sân:  http://localhost:8080/api/bookings");
        System.out.println("==================================================");
    }
}

