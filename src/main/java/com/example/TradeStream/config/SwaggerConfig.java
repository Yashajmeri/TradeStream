package com.example.TradeStream.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI tradeStreamOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TradeStream API")
                        .description("""
                                Cryptocurrency and stock trading REST API.

                                **Authentication:** Sign in via `POST /auth/signin` to receive a JWT token, \
                                then click **Authorize** and enter `<your-token>` in the Bearer field.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("TradeStream")
                                .email("ajmeriyash1312@gmail.com")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SCHEME_NAME, new SecurityScheme()
                                .name(SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the JWT token from /auth/signin")));
    }
}
