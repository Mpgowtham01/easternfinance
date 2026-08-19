package com.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class GatewayService {

	public static void main(String[] args)
	{
		var context = SpringApplication.run(GatewayService.class, args);
		ConfigurableEnvironment env = (ConfigurableEnvironment) context.getEnvironment();
		String activeProfile = env.getActiveProfiles().length > 0 ? env.getActiveProfiles()[0] : "default";

		if (activeProfile.equalsIgnoreCase("prod"))
		{
			System.out.println("✅ Gateway Production Server started!");
		} else if (activeProfile.equalsIgnoreCase("dev"))
		{
			System.out.println("✅ Gateway Development Server started!");
		} else
		{
			System.out.println("✅ Gateway "+activeProfile+" Server started!");
		}
	}

	@RestController
	public class FallbackController {
		@RequestMapping("/")
		public String home() {
			return "Welcome to Easternfin - Microservice API Gateway!";
		}
	}

}
