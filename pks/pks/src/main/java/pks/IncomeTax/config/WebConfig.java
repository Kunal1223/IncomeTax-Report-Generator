package pks.IncomeTax.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Comma-separated list of allowed origins/patterns.
     * Example: https://your-domain.com,https://www.your-domain.com
     * Default: *
     */
    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Allow the deployed UI (different host/port) to call the API.
        // NOTE: We disable credentials so we can safely allow broad origins.
        String[] origins = java.util.Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toArray(String[]::new);

        registry.addMapping("/api/**")
            .allowedOriginPatterns(origins.length == 0 ? new String[] {"*"} : origins)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders("Content-Disposition")
            .allowCredentials(false)
            .maxAge(3600);
    }
}
