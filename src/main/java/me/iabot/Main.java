package me.iabot;

import me.iabot.commands.BotCommand;
import me.iabot.commands.AdminCommand;
import me.iabot.web.DashboardServer;
import me.iabot.utils.DatabaseManager;
import me.iabot.utils.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class Main extends JavaPlugin {
    private static Main instance;
    private BotManager botManager;
    private DashboardServer dashboardServer;
    private DatabaseManager databaseManager;
    private ConfigManager configManager;
    private BukkitTask saveTask;

    @Override
    public void onEnable() {
        instance = this;
        
        // Carregar configuração
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        
        // Inicializar banco de dados
        databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            getLogger().severe("Falha ao inicializar banco de dados!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Inicializar gerenciador de bots
        botManager = new BotManager(this);
        
        // Carregar bots salvos
        botManager.loadBotsFromDatabase();
        
        // Registrar comandos
        getCommand("bot").setExecutor(new BotCommand(this));
        getCommand("aiadmin").setExecutor(new AdminCommand(this));
        
        // Iniciar dashboard
        startDashboard();
        
        // Task de salvamento automático
        startAutoSave();
        
        getLogger().info("Advanced IA Bot v2.0 ativado!");
        getLogger().info("Dashboard: http://0.0.0.0:" + getConfig().getInt("web.port"));
    }

    @Override
    public void onDisable() {
        // Salvar todos os bots
        if (botManager != null) {
            botManager.saveAllBots();
        }
        
        // Parar dashboard
        if (dashboardServer != null) {
            dashboardServer.stop();
        }
        
        // Parar tasks
        if (saveTask != null) {
            saveTask.cancel();
        }
        
        // Fechar conexão do banco de dados
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        getLogger().info("Advanced IA Bot desativado!");
    }

    private void startDashboard() {
        try {
            int port = getConfig().getInt("web.port", 8080);
            dashboardServer = new DashboardServer(this, port);
            dashboardServer.start();
        } catch (Exception e) {
            getLogger().severe("Falha ao iniciar dashboard: " + e.getMessage());
        }
    }

    private void startAutoSave() {
        saveTask = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (botManager != null) {
                botManager.saveAllBots();
            }
        }, 6000L, 6000L); // A cada 5 minutos
    }

    public static Main getInstance() {
        return instance;
    }

    public BotManager getBotManager() {
        return botManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}