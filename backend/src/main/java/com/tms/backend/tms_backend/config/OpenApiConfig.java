package com.tms.backend.tms_backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI ticketManagementOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Ticket Management System API")
						.version("1.0.0")
						.description("Ticket Management System - Created by Simran. Comprehensive API for ticket lifecycle and comment management.")
						.contact(new Contact().name("Simran")));
	}
}