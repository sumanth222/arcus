package com.arcus.arc1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class Arc1Application {

	public static void main(String[] args) {
		SpringApplication.run(Arc1Application.class, args);
	}

}