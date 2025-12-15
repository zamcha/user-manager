package vn.tqd.mobilemall.usermanager.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    // 1. Inject giá trị từ application.yml
    @Value("${api-docs.server}")
    private String serverUrl;

    @Value("${api-docs.oauth2.authorization-url}")
    private String authUrl;

    @Value("${api-docs.oauth2.token-url}")
    private String tokenUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // 2. Cấu hình thông tin chung (Info)
                .info(new Info()
                        .title("User Manager API Service")
                        .version("1.0")
                        .description("API Documents for User Manager Service")
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")))

                // 3. Cấu hình Server (Quan trọng khi chạy sau Gateway)
                // Giúp Swagger biết prefix /user-manager để gọi API đúng
                .servers(List.of(new Server().url(serverUrl).description("Gateway Server")))

                // 4. Cấu hình Security (OAuth2)
                .components(new Components()
                        .addSecuritySchemes("oauth2", new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .description("Sử dụng OAuth2 Login qua Gateway")
                                .flows(new OAuthFlows()
                                        .authorizationCode(new OAuthFlow()
                                                .authorizationUrl(authUrl)
                                                .tokenUrl(tokenUrl)
                                                .scopes(new Scopes()

                                                        .addString("profile", "Profile info")
                                                        )))))

                // 5. Áp dụng Security global cho toàn bộ API
                .security(List.of(new SecurityRequirement().addList("oauth2")));
    }
}