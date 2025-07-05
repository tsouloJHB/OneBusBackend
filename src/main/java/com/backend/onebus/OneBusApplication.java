package com.backend.onebus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OneBusApplication {

	public static void main(String[] args) {
		SpringApplication.run(OneBusApplication.class, args);
	}

}
