package com.marketflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI marketflowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MARKETFLOW REST API")
                        .description("AI-Powered Visual Workflow Automation Backend (ALG-AUTO-01)")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("MARKETFLOW Team")
                                .email("dev@marketflow.io"))
                        .license(new License().name("MIT License")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development Server"),
                        new Server().url("https://api.marketflow.io").description("Production Server")
                ));
    }
}
