package com.danielkleyman.jobsearchalljobs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JobSearchAlljobsApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobSearchAlljobsApplication.class, args);
    }

}
