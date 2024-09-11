package com.danielkleyman.jobsearchind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JobSearchIndApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobSearchIndApplication.class, args);
    }

}
