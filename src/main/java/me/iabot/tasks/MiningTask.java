package me.iabot.tasks;

import me.iabot.BotAI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Arrays;

public class MiningTask {
    private BotAI bot;
    private Location location;
    
    public MiningTask(BotAI bot, Location location) {
        this.bot = bot;
        this.location = location;
    }
    
    public boolean execute() {
        World world = location.getWorld();
        int maxDepth = bot.getPlugin().getConfig().getInt("tasks.mining.max-depth", 64);
        
        // Verificar blocos valiosos próximos
        List<Material> valuableBlocks = Arrays.asList(
            Material.DIAMOND_ORE,
            Material.GOLD_ORE,
            Material.IRON_ORE,
            Material.COAL_ORE,
            Material.LAPIS_ORE,
            Material.REDSTONE_ORE
        );
        
        // Procurar em um raio de 3 blocos
        for (int x = -3; x <= 3; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -3; z <= 3; z++) {
                    Block block = world.getBlockAt(
                        location.getBlockX() + x,
                        location.getBlockY() + y,
                        location.getBlockZ() + z
                    );
                    
                    if (valuableBlocks.contains(block.getType()) && block.getY() > maxDepth) {
                        // Minerar o bloco
                        Material material = block.getType();
                        bot.getInventory().addItem(new ItemStack(material, 1));
                        block.setType(Material.AIR);
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
}