package com.example.horsearchery;

import com.example.horsearchery.config.HorseArcheryConfig;
import net.fabricmc.api.ClientModInitializer;

public class HorseArchery implements ClientModInitializer {
    public static HorseArcheryConfig config;
    
    @Override
    public void onInitializeClient() {
        // Load config on mod initialization
        config = HorseArcheryConfig.load();
        config.save(); // Save to create default config file if it doesn't exist
    }
}

