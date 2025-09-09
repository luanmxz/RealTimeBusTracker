package com.devluanmarcene.NextBusRealTimeTracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class NextBusRealTimeTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(NextBusRealTimeTrackerApplication.class, args);
	}

}
