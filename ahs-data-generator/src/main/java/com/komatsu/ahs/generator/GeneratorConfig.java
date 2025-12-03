package com.komatsu.ahs.generator;

import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.util.Map;

public class GeneratorConfig {
    private int vehicles;
    private long interval;

    public GeneratorConfig() {
        loadConfig();
    }

    private void loadConfig() {
        Yaml yaml = new Yaml();
        try (InputStream in = this.getClass()
                .getClassLoader()
                .getResourceAsStream("application.yml")) {
            Map<String, Object> config = yaml.load(in);
            Map<String, Object> generatorConfig = (Map<String, Object>) config.get("generator");
            this.vehicles = (int) generatorConfig.get("vehicles");
            this.interval = ((Number) generatorConfig.get("interval")).longValue();
        } catch (Exception e) {
            // Fallback to defaults if config file is not found or invalid
            this.vehicles = 15;
            this.interval = 5000;
        }
    }

    public int getVehicles() {
        return vehicles;
    }

    public long getInterval() {
        return interval;
    }
}
