package com.example.bickdemo.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads local .env files as a low-priority property source.
 * This keeps real environment variables taking precedence while making
 * local runs from either the repo root or backend module pick up secrets.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "bickdemoDotenv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> properties = loadDotenvProperties();
        if (properties.isEmpty()) {
            return;
        }

        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            environment.getPropertySources().remove(PROPERTY_SOURCE_NAME);
        }
        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private Map<String, Object> loadDotenvProperties() {
        LinkedHashMap<String, Object> properties = new LinkedHashMap<>();
        Path userDir = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();

        Set<Path> candidates = new LinkedHashSet<>();
        candidates.add(userDir.resolve("..").resolve(".env").normalize());
        candidates.add(userDir.resolve(".env").normalize());
        candidates.add(userDir.resolve("bickdemo-backend").resolve(".env").normalize());

        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                loadFile(candidate, properties);
            }
        }

        return properties;
    }

    private void loadFile(Path path, Map<String, Object> properties) {
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int separatorIndex = line.indexOf('=');
                if (separatorIndex <= 0) {
                    continue;
                }

                String key = line.substring(0, separatorIndex).trim();
                String value = stripQuotes(line.substring(separatorIndex + 1).trim());
                if (!key.isEmpty()) {
                    properties.put(key, value);
                }
            }
        } catch (IOException ignored) {
            // Ignore unreadable local .env files and fall back to normal config resolution.
        }
    }

    private String stripQuotes(String value) {
        if (value.length() >= 2) {
            boolean doubleQuoted = value.startsWith("\"") && value.endsWith("\"");
            boolean singleQuoted = value.startsWith("'") && value.endsWith("'");
            if (doubleQuoted || singleQuoted) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
