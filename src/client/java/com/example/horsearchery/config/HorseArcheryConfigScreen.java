package com.example.horsearchery.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class HorseArcheryConfigScreen {
    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("title.horsearchery.config"));
        
        HorseArcheryConfig config = HorseArcheryConfig.get();
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        
        ConfigCategory general = builder.getOrCreateCategory(Text.translatable("category.horsearchery.general"));
        
        general.addEntry(entryBuilder.startIntSlider(Text.translatable("option.horsearchery.yawRotationSpeed"), config.yawRotationSpeed, 0, 100)
                .setDefaultValue(15)
                .setTooltip(Text.translatable("tooltip.horsearchery.yawRotationSpeed"))
                .setSaveConsumer(newValue -> {
                    config.yawRotationSpeed = newValue;
                    AutoConfig.getConfigHolder(HorseArcheryConfig.class).save();
                })
                .build());
        
        builder.setSavingRunnable(() -> {
            AutoConfig.getConfigHolder(HorseArcheryConfig.class).save();
        });
        
        return builder.build();
    }
}

