package com.airtasker.challenge;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan
public class RatelimiterApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(RatelimiterApplication.class)
				.profiles("dev", "prod")
		.run(args);
	}

}
