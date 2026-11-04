package ma.ump.deptinfo.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Plateforme de Gestion du Département Informatique (PGDI 2026)")
                        .description("Documentation interactive des APIs REST — Master Génie Logiciel (UMP FSO). " +
                                "Permet de tester tous les endpoints avec authentification JWT.")
                        .version("v2.0")
                        .contact(new Contact()
                                .name("Équipe PGDI 2026")
                                .email("contact@pgdi.ump.ma"))
                        .license(new License()
                                .name("Usage Académique")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Entrez votre jeton JWT (obtenu via POST /api/auth/login) pour vous authentifier.")));
    }
}
