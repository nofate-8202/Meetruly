package com.meetruly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MeetrulyApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeetrulyApplication.class, args);
    }

}
