package com.predykt.accounting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PredyktAccountingApplication {

	public static void main(String[] args) {
		SpringApplication.run(PredyktAccountingApplication.class, args);
	}

}
