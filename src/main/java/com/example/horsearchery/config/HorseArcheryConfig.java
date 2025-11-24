package com.example.horsearchery.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class HorseArcheryConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "horsearchery.json");
    
    public float yawRotationSpeed = 0.15f; // Default rotation speed (0.0-1.0)
    
    public static HorseArcheryConfig load() {
        HorseArcheryConfig config = new HorseArcheryConfig();
        
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                config = GSON.fromJson(reader, HorseArcheryConfig.class);
                if (config == null) {
                    config = new HorseArcheryConfig();
                }
            } catch (IOException e) {
                System.err.println("Failed to load Horse Archery config: " + e.getMessage());
                config = new HorseArcheryConfig();
            }
        }
        
        // Ensure the value is within valid range
        config.yawRotationSpeed = Math.max(0.0f, Math.min(1.0f, config.yawRotationSpeed));
        
        return config;
    }
    
    public void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save Horse Archery config: " + e.getMessage());
        }
    }
}

