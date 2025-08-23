package net.brodino.petsinabag.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.brodino.petsinabag.PetsInABag;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private Path configFilePath;
    private ConfigData data;
    
    public Config() {

        Path dataDirectory = Path.of("config/petsinabag");

        try {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }
            this.configFilePath = dataDirectory.resolve("config.json");
            this.load();
        } catch (IOException e) {
            PetsInABag.LOGGER.error("Failed to load Config file: {}", e.getMessage());
        }

    }

    private void load() throws IOException {
        if (!Files.exists(configFilePath)) {
            data = getDefaults();
            this.save();
        } else {
            try (Reader reader = Files.newBufferedReader(configFilePath)) {
                data = GSON.fromJson(reader, ConfigData.class);
                if (data == null) {
                    data = getDefaults();
                    this.save();
                }
            }
        }
    }
    
    public void reload() throws IOException {
        load();
    }
    
    private void save() throws IOException {
        try (Writer writer = Files.newBufferedWriter(configFilePath)) {
            GSON.toJson(data, writer);
        }
    }
    
    private ConfigData getDefaults() {
        ConfigData defaults = new ConfigData();
        defaults.allowedPets = List.of("minecraft:cat", "minecraft:axolotl");
        defaults.allowedDimensions = List.of("minecraft:overworld");
        return defaults;
    }
    
    public List<String> getAllowedPets() {
        return data.allowedPets;
    }
    
    public List<String> getAllowedDimensions() {
        return data.allowedDimensions;
    }
}