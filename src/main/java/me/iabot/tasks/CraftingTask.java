package me.iabot.tasks;

import me.iabot.BotAI;
import me.iabot.BotInventory;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class CraftingTask {
    private BotAI bot;
    
    public CraftingTask(BotAI bot) {
        this.bot = bot;
    }
    
    public boolean execute() {
        BotInventory inventory = bot.getInventory();
        
        // Exemplo simples: craftar tochas se tiver carvão e gravetos
        if (inventory.hasItem(Material.COAL, 1) && inventory.hasItem(Material.STICK, 1)) {
            inventory.removeItem(Material.COAL, 1);
            inventory.removeItem(Material.STICK, 1);
            inventory.addItem(new ItemStack(Material.TORCH, 4));
            return true;
        }
        
        // Craftar gravetos se tiver madeira
        if (inventory.hasItem(Material.OAK_LOG, 1)) {
            inventory.removeItem(Material.OAK_LOG, 1);
            inventory.addItem(new ItemStack(Material.STICK, 4));
            return true;
        }
        
        return false;
    }
}