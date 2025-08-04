package me.iabot;

import me.iabot.utils.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BotManager {
    private Main plugin;
    private Map<String, BotAI> activeBots = new ConcurrentHashMap<>();
    private Map<String, BukkitTask> botTasks = new ConcurrentHashMap<>();
    private DatabaseManager databaseManager;

    public BotManager(Main plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    public boolean createBot(String name, Location location) {
        if (activeBots.containsKey(name)) {
            return false;
        }

        if (activeBots.size() >= plugin.getConfig().getInt("bots.max-bots", 50)) {
            return false;
        }

        BotAI bot = new BotAI(plugin, name, location);
        activeBots.put(name, bot);
        
        // Iniciar task do bot
        BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(
            plugin, 
            bot, 
            20L, 
            plugin.getConfig().getLong("bots.update-interval", 20L)
        );
        botTasks.put(name, task);
        
        // Salvar no banco de dados
        databaseManager.saveBot(bot);
        
        return true;
    }

    public boolean removeBot(String name) {
        BotAI bot = activeBots.remove(name);
        BukkitTask task = botTasks.remove(name);
        
        if (task != null) {
            task.cancel();
        }
        
        if (bot != null) {
            databaseManager.deleteBot(name);
            return true;
        }
        
        return false;
    }

    public BotAI getBot(String name) {
        return activeBots.get(name);
    }

    public Collection<BotAI> getAllBots() {
        return new ArrayList<>(activeBots.values());
    }

    public Map<String, BotAI> getActiveBots() {
        return new HashMap<>(activeBots);
    }

    public void saveAllBots() {
        for (BotAI bot : activeBots.values()) {
            databaseManager.saveBot(bot);
        }
    }

    public void loadBotsFromDatabase() {
        List<BotAI> bots = databaseManager.loadAllBots();
        for (BotAI bot : bots) {
            activeBots.put(bot.getName(), bot);
            
            // Iniciar task do bot
            BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin, 
                bot, 
                20L, 
                plugin.getConfig().getLong("bots.update-interval", 20L)
            );
            botTasks.put(bot.getName(), task);
        }
        
        plugin.getLogger().info("Carregados " + bots.size() + " bots do banco de dados");
    }

    public int getBotCount() {
        return activeBots.size();
    }

    public void shutdown() {
        saveAllBots();
        for (BukkitTask task : botTasks.values()) {
            task.cancel();
        }
        botTasks.clear();
        activeBots.clear();
    }
}