package me.iabot.utils;

import me.iabot.Main;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    private Main plugin;
    private FileConfiguration config;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        validateConfig();
    }

    private void validateConfig() {
        // Validar e definir valores padrão
        if (!config.contains("gemini.api-key")) {
            config.set("gemini.api-key", "SUA_API_KEY_AQUI");
        }
        
        if (!config.contains("web.port")) {
            config.set("web.port", 8080);
        }
        
        if (!config.contains("bots.max-bots")) {
            config.set("bots.max-bots", 50);
        }
        
        plugin.saveConfig();
    }

    public String getGeminiAPIKey() {
        return config.getString("gemini.api-key", "");
    }

    public int getWebPort() {
        return config.getInt("web.port", 8080);
    }

    public String getAdminPassword() {
        return config.getString("web.admin-password", "admin123");
    }

    public int getMaxBots() {
        return config.getInt("bots.max-bots", 50);
    }

    public boolean isLearningEnabled() {
        return config.getBoolean("bots.enable-learning", true);
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        validateConfig();
    }
}