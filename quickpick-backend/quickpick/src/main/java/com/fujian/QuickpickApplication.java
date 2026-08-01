package com.fujian;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.fujian.mapper")
@EnableScheduling
@EnableAsync
public class QuickpickApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuickpickApplication.class, args);
    }

}
