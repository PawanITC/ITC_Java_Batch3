package com.itc.funkart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FunkartApplication {

	public static void main(String[] args) {
		SpringApplication.run(FunkartApplication.class, args);
	}

}
