package com.example.snapUp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SnapUpApplication {

	public static void main(String[] args) {
		SpringApplication.run(SnapUpApplication.class, args);
	}

}
