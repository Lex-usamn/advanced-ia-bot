package me.iabot.web;

import me.iabot.Main;
import me.iabot.BotAI;
import me.iabot.BotManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class WebAPI {
    private Main plugin;
    private Gson gson;

    public WebAPI(Main plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
    }

    public String getBotsList() {
        JsonObject response = new JsonObject();
        JsonArray botsArray = new JsonArray();
        
        Collection<BotAI> bots = plugin.getBotManager().getAllBots();
        
        for (BotAI bot : bots) {
            JsonObject botObj = new JsonObject();
            botObj.addProperty("name", bot.getName());
            botObj.addProperty("level", bot.getLevel());
            botObj.addProperty("experience", bot.getExperience());
            botObj.addProperty("task", bot.getCurrentTask());
            botObj.addProperty("online", bot.isOnline());
            
            JsonObject location = new JsonObject();
            location.addProperty("x", bot.getLocation().getX());
            location.addProperty("y", bot.getLocation().getY());
            location.addProperty("z", bot.getLocation().getZ());
            location.addProperty("world", bot.getLocation().getWorld().getName());
            botObj.add("location", location);
            
            botsArray.add(botObj);
        }
        
        response.add("bots", botsArray);
        response.addProperty("total", bots.size());
        return gson.toJson(response);
    }

    public String getBotDetails(String botName) {
        BotAI bot = plugin.getBotManager().getBot(botName);
        
        if (bot == null) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "Bot não encontrado");
            return gson.toJson(error);
        }
        
        JsonObject response = new JsonObject();
        response.addProperty("name", bot.getName());
        response.addProperty("level", bot.getLevel());
        response.addProperty("experience", bot.getExperience());
        response.addProperty("task", bot.getCurrentTask());
        response.addProperty("online", bot.isOnline());
        
        JsonObject location = new JsonObject();
        location.addProperty("x", bot.getLocation().getX());
        location.addProperty("y", bot.getLocation().getY());
        location.addProperty("z", bot.getLocation().getZ());
        location.addProperty("world", bot.getLocation().getWorld().getName());
        response.add("location", location);
        
        JsonObject inventory = new JsonObject();
        for (Map.Entry<org.bukkit.Material, Integer> entry : bot.getInventory().getItems().entrySet()) {
            inventory.addProperty(entry.getKey().name(), entry.getValue());
        }
        response.add("inventory", inventory);
        
        JsonObject skills = new JsonObject();
        for (Map.Entry<String, Integer> entry : bot.getSkillLevels().entrySet()) {
            skills.addProperty(entry.getKey(), entry.getValue());
        }
        response.add("skills", skills);
        
        return gson.toJson(response);
    }

    public String createBot(String requestData) {
        try {
            JsonObject request = gson.fromJson(requestData, JsonObject.class);
            String name = request.get("name").getAsString();
            
            // Encontrar posição do primeiro jogador online ou usar spawn padrão
            Location spawnLocation;
            Player player = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
            
            if (player != null) {
                spawnLocation = player.getLocation();
            } else {
                World world = Bukkit.getWorlds().get(0);
                spawnLocation = world.getSpawnLocation();
            }
            
            boolean success = plugin.getBotManager().createBot(name, spawnLocation);
            
            JsonObject response = new JsonObject();
            response.addProperty("success", success);
            response.addProperty("message", success ? "Bot criado com sucesso" : "Falha ao criar bot");
            
            return gson.toJson(response);
            
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("success", false);
            error.addProperty("message", "Dados inválidos: " + e.getMessage());
            return gson.toJson(error);
        }
    }

    public String removeBot(String botName) {
        boolean success = plugin.getBotManager().removeBot(botName);
        
        JsonObject response = new JsonObject();
        response.addProperty("success", success);
        response.addProperty("message", success ? "Bot removido com sucesso" : "Bot não encontrado");
        
        return gson.toJson(response);
    }

    public String getStats() {
        JsonObject stats = new JsonObject();
        
        stats.addProperty("totalBots", plugin.getBotManager().getBotCount());
        stats.addProperty("maxBots", plugin.getConfig().getInt("bots.max-bots"));
        stats.addProperty("uptime", System.currentTimeMillis() - plugin.getServer().getStartTime());
        
        JsonObject serverStats = new JsonObject();
        serverStats.addProperty("onlinePlayers", Bukkit.getOnlinePlayers().size());
        serverStats.addProperty("maxPlayers", Bukkit.getMaxPlayers());
        stats.add("server", serverStats);
        
        return gson.toJson(stats);
    }
}