package com.backend.onebus.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Development server"),
                        new Server().url("https://api.onebus.com").description("Production server")
                ))
                .info(new Info()
                        .title("OneBus Backend API")
                        .description("API for OneBus - Real-time bus tracking and route management system")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("OneBus Development Team")
                                .email("dev@onebus.com")
                                .url("https://github.com/tsouloJHB/OneBusBackend"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
