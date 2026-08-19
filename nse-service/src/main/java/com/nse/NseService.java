package com.nse;

import java.util.List;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableFeignClients
public class NseService {

	@Value("${custom.server.url}")
	private String serverUrl;

	public static void main(String[] args)
	{
		var context = SpringApplication.run(NseService.class, args);
		ConfigurableEnvironment env = (ConfigurableEnvironment) context.getEnvironment();
		String activeProfile = env.getActiveProfiles().length > 0 ? env.getActiveProfiles()[0] : "default";

		if (activeProfile.equalsIgnoreCase("prod"))
		{
			System.out.println("✅ NSE Production Server started!");
		} else if (activeProfile.equalsIgnoreCase("dev"))
		{
			System.out.println("✅ NSE Development Server started!");
		} else
		{
			System.out.println("✅ NSE "+activeProfile+" Server started!");
		}
	}

	@Bean
	public OpenAPI customOpenAPI() {
		SecurityScheme bearerScheme = new SecurityScheme()
				.type(SecurityScheme.Type.HTTP)
				.scheme("bearer")
				.bearerFormat("JWT");

		SecurityRequirement globalReq = new SecurityRequirement()
				.addList("bearerAuth");

		return new OpenAPI()
				.components(new Components()
						.addSecuritySchemes("bearerAuth", bearerScheme))
				.addSecurityItem(globalReq)
				.info(new Info()
						.title("NSE Service API")
						.version("v1.0")
						.description("""
                            This API handles NSE-related operations.

                            It provides endpoints to create, update, and fetch products.

                            Designed to be consumed by frontend and other microservices.
                            """))
				.servers(List.of(
						new Server()
								.url(serverUrl)
								.description("Local dev server")
				));
	}

	@RestController
	public class FallbackController
	{
		@Hidden
		@RequestMapping("/")
		public String home() {
			return "Welcome to Easternfin - Microservice NSE Service!";
		}
	}
}
