package me.iabot;

import me.iabot.utils.GeminiAPI;
import me.iabot.tasks.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class BotAI implements Runnable {
    private Main plugin;
    private String name;
    private Location location;
    private int level;
    private int experience;
    private String currentTask;
    private long lastActionTime;
    private BotInventory inventory;
    private LearningSystem learningSystem;
    private Map<String, Integer> skillLevels;
    private Map<String, Long> cooldowns;
    private boolean isOnline;

    public BotAI(Main plugin, String name, Location location) {
        this.plugin = plugin;
        this.name = name;
        this.location = location.clone();
        this.level = 1;
        this.experience = 0;
        this.currentTask = "idle";
        this.lastActionTime = System.currentTimeMillis();
        this.inventory = new BotInventory();
        this.learningSystem = new LearningSystem(this);
        this.skillLevels = new HashMap<>();
        this.cooldowns = new HashMap<>();
        this.isOnline = true;
        
        // Inicializar skills
        initializeSkills();
    }

    private void initializeSkills() {
        skillLevels.put("mining", 1);
        skillLevels.put("building", 1);
        skillLevels.put("crafting", 1);
        skillLevels.put("combat", 1);
        skillLevels.put("exploration", 1);
        skillLevels.put("farming", 1);
    }

    @Override
    public void run() {
        if (!isOnline) return;
        
        try {
            // Verificar cooldowns
            checkCooldowns();
            
            // Atualizar contexto para a IA
            String context = buildContext();
            
            // Obter decisão da IA (Gemini)
            String decision = GeminiAPI.getDecision(plugin, context);
            
            // Executar ação
            executeDecision(decision);
            
            // Atualizar sistema de aprendizado
            learningSystem.update();
            
            // Ganhar experiência passiva
            gainExperience(1);
            
            // Atualizar timestamp
            lastActionTime = System.currentTimeMillis();
            
        } catch (Exception e) {
            plugin.getLogger().warning("Erro na IA do bot " + name + ": " + e.getMessage());
            executeRandomAction();
        }
    }

    private String buildContext() {
        StringBuilder context = new StringBuilder();
        context.append("Bot ").append(name).append(" (Level ").append(level).append(")\n");
        context.append("Localização: ").append(formatLocation()).append("\n");
        context.append("Tarefa atual: ").append(currentTask).append("\n");
        context.append("Inventário: ").append(inventory.toString()).append("\n");
        context.append("Skills: ").append(formatSkills()).append("\n");
        context.append("Experiência: ").append(experience).append("/").append(level * 100).append("\n");
        context.append("Tempo online: ").append(formatUptime()).append("\n\n");
        context.append("Escolha a próxima ação entre: mover, minerar, construir, craftar, combater, coletar, explorar, idle");
        return context.toString();
    }

    private void executeDecision(String decision) {
        currentTask = decision.toLowerCase().trim();
        
        // Verificar cooldown
        if (isOnCooldown(currentTask)) {
            currentTask = "idle";
        }
        
        switch (currentTask) {
            case "mover":
                executeMove();
                break;
            case "minerar":
                executeMining();
                break;
            case "construir":
                executeBuilding();
                break;
            case "craftar":
                executeCrafting();
                break;
            case "combater":
                executeCombat();
                break;
            case "coletar":
                executeCollecting();
                break;
            case "explorar":
                executeExploration();
                break;
            default:
                executeIdle();
                break;
        }
        
        // Aplicar cooldown
        setCooldown(currentTask, 5000); // 5 segundos
    }

    private void executeMove() {
        double range = plugin.getConfig().getDouble("bots.spawn-radius", 10);
        double newX = location.getX() + (ThreadLocalRandom.current().nextDouble() - 0.5) * range;
        double newZ = location.getZ() + (ThreadLocalRandom.current().nextDouble() - 0.5) * range;
        double newY = location.getWorld().getHighestBlockYAt((int)newX, (int)newZ) + 1;
        
        location.setX(newX);
        location.setY(newY);
        location.setZ(newZ);
        
        gainExperience(2);
        improveSkill("exploration", 1);
    }

    private void executeMining() {
        MiningTask miningTask = new MiningTask(this, location);
        boolean success = miningTask.execute();
        
        if (success) {
            gainExperience(10);
            improveSkill("mining", 2);
        }
    }

    private void executeBuilding() {
        // Implementação simplificada
        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        
        // Construir uma pequena estrutura
        for (int i = 0; i < 3; i++) {
            world.getBlockAt(x, y + i, z).setType(Material.COBBLESTONE);
        }
        
        gainExperience(15);
        improveSkill("building", 2);
    }

    private void executeCrafting() {
        CraftingTask craftingTask = new CraftingTask(this);
        boolean success = craftingTask.execute();
        
        if (success) {
            gainExperience(20);
            improveSkill("crafting", 3);
        }
    }

    private void executeCombat() {
        CombatTask combatTask = new CombatTask(this, location);
        boolean success = combatTask.execute();
        
        if (success) {
            gainExperience(25);
            improveSkill("combat", 3);
        }
    }

    private void executeCollecting() {
        // Coletar recursos próximos
        World world = location.getWorld();
        int radius = 5;
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Block block = world.getBlockAt(
                    location.getBlockX() + x,
                    location.getBlockY(),
                    location.getBlockZ() + z
                );
                
                if (isValuableBlock(block)) {
                    inventory.addItem(new ItemStack(block.getType(), 1));
                    block.setType(Material.AIR);
                    gainExperience(5);
                }
            }
        }
        
        improveSkill("exploration", 1);
    }

    private void executeExploration() {
        executeMove(); // Exploração é basicamente movimento inteligente
        gainExperience(3);
        improveSkill("exploration", 1);
    }

    private void executeIdle() {
        // Não fazer nada
    }

    private void executeRandomAction() {
        String[] actions = {"mover", "minerar", "construir", "coletar", "explorar", "idle"};
        String action = actions[ThreadLocalRandom.current().nextInt(actions.length)];
        executeDecision(action);
    }

    private boolean isValuableBlock(Block block) {
        Material type = block.getType();
        return type == Material.DIAMOND_ORE || type == Material.GOLD_ORE || 
               type == Material.IRON_ORE || type == Material.COAL_ORE ||
               type == Material.LAPIS_ORE || type == Material.REDSTONE_ORE;
    }

    private void gainExperience(int amount) {
        int multiplier = plugin.getConfig().getInt("bots.experience-multiplier", 1);
        experience += amount * multiplier;
        
        if (experience >= level * 100) {
            levelUp();
        }
    }

    private void levelUp() {
        level++;
        experience = 0;
        plugin.getLogger().info("Bot " + name + " subiu para o nível " + level);
    }

    private void improveSkill(String skill, int amount) {
        skillLevels.put(skill, skillLevels.getOrDefault(skill, 1) + amount);
    }

    private String formatLocation() {
        return String.format("X:%.1f Y:%.1f Z:%.1f Mundo:%s", 
            location.getX(), location.getY(), location.getZ(), location.getWorld().getName());
    }

    private String formatSkills() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : skillLevels.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(entry.getKey()).append(":").append(entry.getValue());
        }
        return sb.toString();
    }

    private String formatUptime() {
        long uptime = System.currentTimeMillis() - lastActionTime;
        return String.format("%d segundos", uptime / 1000);
    }

    // Sistema de cooldown
    private void checkCooldowns() {
        long now = System.currentTimeMillis();
        cooldowns.entrySet().removeIf(entry -> entry.getValue() < now);
    }

    private boolean isOnCooldown(String action) {
        Long cooldownEnd = cooldowns.get(action);
        return cooldownEnd != null && cooldownEnd > System.currentTimeMillis();
    }

    private void setCooldown(String action, long duration) {
        cooldowns.put(action, System.currentTimeMillis() + duration);
    }

    // Getters e Setters
    public String getName() { return name; }
    public Location getLocation() { return location.clone(); }
    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public String getCurrentTask() { return currentTask; }
    public long getLastActionTime() { return lastActionTime; }
    public BotInventory getInventory() { return inventory; }
    public Map<String, Integer> getSkillLevels() { return new HashMap<>(skillLevels); }
    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { this.isOnline = online; }
    public void setLocation(Location location) { this.location = location.clone(); }
    public void setCurrentTask(String task) { this.currentTask = task; }
}