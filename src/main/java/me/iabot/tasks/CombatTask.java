package me.iabot.tasks;

import me.iabot.BotAI;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;

public class CombatTask {
    private BotAI bot;
    private Location location;
    
    public CombatTask(BotAI bot, Location location) {
        this.bot = bot;
        this.location = location;
    }
    
    public boolean execute() {
        World world = location.getWorld();
        double radius = bot.getPlugin().getConfig().getDouble("tasks.combat.aggro-radius", 16);
        
        // Procurar mobs hostis próximos
        for (Entity entity : world.getNearbyEntities(location, radius, radius, radius)) {
            if (entity instanceof Monster && entity.isValid()) {
                Monster monster = (Monster) entity;
                
                // "Atacar" o mob (na prática, apenas simular)
                if (monster.getHealth() > 0) {
                    // Reduzir vida do mob (simulação)
                    return true;
                }
            }
        }
        
        return false;
    }
}