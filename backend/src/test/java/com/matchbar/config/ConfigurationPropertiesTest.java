package com.matchbar.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Al actualizar Spring Boot algunas propiedades se renombran y la antigua deja
 * de tener efecto sin ningún error (pasó con spring.data.mongodb.uri en Boot 4:
 * la API intentaba conectar a localhost). Este test compara nuestra
 * configuración con los metadatos de las librerías del classpath.
 */
class ConfigurationPropertiesTest {

    private static final List<String> CONFIG_FILES = List.of("application.yml", "application-dev.yml");

    @Test
    void ningunaPropiedadDeNuestraConfiguracionEstaRetirada() throws IOException {
        Map<String, String> retired = retiredProperties();
        List<String> problems = new ArrayList<>();
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        for (String file : CONFIG_FILES) {
            for (PropertySource<?> source : loader.load(file, new ClassPathResource(file))) {
                for (String key : ((EnumerablePropertySource<?>) source).getPropertyNames()) {
                    if (retired.containsKey(key)) problems.add(file + ": " + key + " -> usa " + retired.get(key));
                }
            }
        }
        assertTrue(problems.isEmpty(), "Propiedades retiradas (Spring las ignora): " + problems);
    }

    /** Propiedades marcadas como retiradas (deprecation level "error") en los metadatos de Spring. */
    private static Map<String, String> retiredProperties() throws IOException {
        JsonMapper mapper = JsonMapper.builder().build();
        Map<String, String> retired = new HashMap<>();
        Enumeration<URL> metadata = ConfigurationPropertiesTest.class.getClassLoader()
                .getResources("META-INF/spring-configuration-metadata.json");
        while (metadata.hasMoreElements()) {
            try (InputStream in = metadata.nextElement().openStream()) {
                for (JsonNode property : mapper.readTree(in).path("properties")) {
                    JsonNode deprecation = property.path("deprecation");
                    if ("error".equals(deprecation.path("level").asString())) {
                        retired.put(property.path("name").asString(), deprecation.path("replacement").asString());
                    }
                }
            }
        }
        return retired;
    }
}
