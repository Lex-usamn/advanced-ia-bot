package me.iabot.utils;

import me.iabot.Main;
import me.iabot.BotAI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {
    private Main plugin;
    private Connection connection;
    private String dbPath;

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
        this.dbPath = plugin.getDataFolder() + "/data/bots.db";
    }

    public boolean initialize() {
        try {
            // Criar diretório se não existir
            File dataFolder = new File(plugin.getDataFolder(), "data");
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            // Conectar ao SQLite
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            
            // Criar tabelas
            createTables();
            
            plugin.getLogger().info("Banco de dados inicializado com sucesso");
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao inicializar banco de dados: " + e.getMessage());
            return false;
        }
    }

    private void createTables() throws SQLException {
        Statement stmt = connection.createStatement();
        
        // Tabela de bots
        String createBotsTable = """
            CREATE TABLE IF NOT EXISTS bots (
                name TEXT PRIMARY KEY,
                level INTEGER DEFAULT 1,
                experience INTEGER DEFAULT 0,
                task TEXT DEFAULT 'idle',
                world TEXT,
                x REAL,
                y REAL,
                z REAL,
                yaw REAL,
                pitch REAL,
                online BOOLEAN DEFAULT 1,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        stmt.execute(createBotsTable);
        
        // Tabela de inventário
        String createInventoryTable = """
            CREATE TABLE IF NOT EXISTS bot_inventory (
                bot_name TEXT,
                material TEXT,
                amount INTEGER,
                FOREIGN KEY (bot_name) REFERENCES bots (name)
            )
            """;
        stmt.execute(createInventoryTable);
        
        // Tabela de skills
        String createSkillsTable = """
            CREATE TABLE IF NOT EXISTS bot_skills (
                bot_name TEXT,
                skill_name TEXT,
                level INTEGER DEFAULT 1,
                FOREIGN KEY (bot_name) REFERENCES bots (name)
            )
            """;
        stmt.execute(createSkillsTable);
        
        stmt.close();
    }

    public void saveBot(BotAI bot) {
        try {
            // Atualizar ou inserir bot
            String upsertBot = """
                INSERT OR REPLACE INTO bots 
                (name, level, experience, task, world, x, y, z, yaw, pitch, online, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;
                
            PreparedStatement stmt = connection.prepareStatement(upsertBot);
            stmt.setString(1, bot.getName());
            stmt.setInt(2, bot.getLevel());
            stmt.setInt(3, bot.getExperience());
            stmt.setString(4, bot.getCurrentTask());
            stmt.setString(5, bot.getLocation().getWorld().getName());
            stmt.setDouble(6, bot.getLocation().getX());
            stmt.setDouble(7, bot.getLocation().getY());
            stmt.setDouble(8, bot.getLocation().getZ());
            stmt.setFloat(9, bot.getLocation().getYaw());
            stmt.setFloat(10, bot.getLocation().getPitch());
            stmt.setBoolean(11, bot.isOnline());
            
            stmt.executeUpdate();
            stmt.close();
            
            // Salvar inventário
            saveBotInventory(bot);
            
            // Salvar skills
            saveBotSkills(bot);
            
        } catch (SQLException e) {
            plugin.getLogger().warning("Erro ao salvar bot " + bot.getName() + ": " + e.getMessage());
        }
    }

    private void saveBotInventory(BotAI bot) throws SQLException {
        // Limpar inventário existente
        String clearInventory = "DELETE FROM bot_inventory WHERE bot_name = ?";
        PreparedStatement clearStmt = connection.prepareStatement(clearInventory);
        clearStmt.setString(1, bot.getName());
        clearStmt.executeUpdate();
        clearStmt.close();
        
        // Inserir itens do inventário
        String insertItem = "INSERT INTO bot_inventory (bot_name, material, amount) VALUES (?, ?, ?)";
        PreparedStatement insertStmt = connection.prepareStatement(insertItem);
        
        for (java.util.Map.Entry<org.bukkit.Material, Integer> entry : bot.getInventory().getItems().entrySet()) {
            insertStmt.setString(1, bot.getName());
            insertStmt.setString(2, entry.getKey().name());
            insertStmt.setInt(3, entry.getValue());
            insertStmt.executeUpdate();
        }
        
        insertStmt.close();
    }

    private void saveBotSkills(BotAI bot) throws SQLException {
        // Limpar skills existentes
        String clearSkills = "DELETE FROM bot_skills WHERE bot_name = ?";
        PreparedStatement clearStmt = connection.prepareStatement(clearSkills);
        clearStmt.setString(1, bot.getName());
        clearStmt.executeUpdate();
        clearStmt.close();
        
        // Inserir skills
        String insertSkill = "INSERT INTO bot_skills (bot_name, skill_name, level) VALUES (?, ?, ?)";
        PreparedStatement insertStmt = connection.prepareStatement(insertSkill);
        
        for (java.util.Map.Entry<String, Integer> entry : bot.getSkillLevels().entrySet()) {
            insertStmt.setString(1, bot.getName());
            insertStmt.setString(2, entry.getKey());
            insertStmt.setInt(3, entry.getValue());
            insertStmt.executeUpdate();
        }
        
        insertStmt.close();
    }

    public List<BotAI> loadAllBots() {
        List<BotAI> bots = new ArrayList<>();
        
        try {
            String query = "SELECT * FROM bots";
            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Location location = loadBotLocation(rs);
                if (location != null) {
                    BotAI bot = new BotAI(plugin, rs.getString("name"), location);
                    bot.setCurrentTask(rs.getString("task"));
                    
                    // Carregar inventário e skills
                    loadBotInventory(bot);
                    loadBotSkills(bot);
                    
                    bots.add(bot);
                }
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            plugin.getLogger().warning("Erro ao carregar bots: " + e.getMessage());
        }
        
        return bots;
    }

    private Location loadBotLocation(ResultSet rs) throws SQLException {
        String worldName = rs.getString("world");
        World world = Bukkit.getWorld(worldName);
        
        if (world == null) {
            // Tentar mundo padrão
            world = Bukkit.getWorlds().get(0);
            if (world == null) return null;
        }
        
        double x = rs.getDouble("x");
        double y = rs.getDouble("y");
        double z = rs.getDouble("z");
        float yaw = rs.getFloat("yaw");
        float pitch = rs.getFloat("pitch");
        
        return new Location(world, x, y, z, yaw, pitch);
    }

    private void loadBotInventory(BotAI bot) {
        try {
            String query = "SELECT material, amount FROM bot_inventory WHERE bot_name = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, bot.getName());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                try {
                    org.bukkit.Material material = org.bukkit.Material.valueOf(rs.getString("material"));
                    int amount = rs.getInt("amount");
                    bot.getInventory().addItem(new org.bukkit.inventory.ItemStack(material, amount));
                } catch (IllegalArgumentException e) {
                    // Material inválido, ignorar
                }
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            plugin.getLogger().warning("Erro ao carregar inventário do bot " + bot.getName());
        }
    }

    private void loadBotSkills(BotAI bot) {
        try {
            String query = "SELECT skill_name, level FROM bot_skills WHERE bot_name = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, bot.getName());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String skillName = rs.getString("skill_name");
                int level = rs.getInt("level");
                // Aqui você precisaria de um método para definir skills diretamente
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            plugin.getLogger().warning("Erro ao carregar skills do bot " + bot.getName());
        }
    }

    public void deleteBot(String botName) {
        try {
            String deleteBot = "DELETE FROM bots WHERE name = ?";
            PreparedStatement stmt = connection.prepareStatement(deleteBot);
            stmt.setString(1, botName);
            stmt.executeUpdate();
            stmt.close();
            
            // Também deletar inventário e skills
            String deleteInventory = "DELETE FROM bot_inventory WHERE bot_name = ?";
            PreparedStatement invStmt = connection.prepareStatement(deleteInventory);
            invStmt.setString(1, botName);
            invStmt.executeUpdate();
            invStmt.close();
            
            String deleteSkills = "DELETE FROM bot_skills WHERE bot_name = ?";
            PreparedStatement skillStmt = connection.prepareStatement(deleteSkills);
            skillStmt.setString(1, botName);
            skillStmt.executeUpdate();
            skillStmt.close();
            
        } catch (SQLException e) {
            plugin.getLogger().warning("Erro ao deletar bot " + botName + ": " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Erro ao fechar conexão do banco de dados: " + e.getMessage());
        }
    }
}