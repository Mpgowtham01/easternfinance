package com.auth;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.List;

@SpringBootApplication
public class AuthService extends SpringBootServletInitializer {

	@Value("${custom.server.url}")
	private String serverUrl;

	public static void main(String[] args)
	{
		var context = SpringApplication.run(AuthService.class, args);
		ConfigurableEnvironment env = (ConfigurableEnvironment) context.getEnvironment();
		String activeProfile = env.getActiveProfiles().length > 0 ? env.getActiveProfiles()[0] : "default";

		if (activeProfile.equalsIgnoreCase("prod"))
		{
			System.out.println("✅ Auth Production Server started!");
		} else if (activeProfile.equalsIgnoreCase("dev"))
		{
			System.out.println("✅ Auth Development Server started!");
		} else
		{
			System.out.println("✅ Auth "+activeProfile+" Server started!");
		}
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(AuthService.class);
	}

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Auth Service API")
						.version("v1.0")
						.description("""
                        This API handles auth-related operations.

                        It provides endpoints to create, update, and fetch auth.

                        Designed to be consumed by frontend and other microservices.
                        """))
				.servers(List.of(new Server().url(serverUrl).description("Local dev server")));
	}

	@RestController
	public class FallbackController
	{
		@Hidden
		@RequestMapping("/")
		public String home() {
			return "Welcome to Easternfin - Microservice Auth Seervice!";
		}
	}
}
