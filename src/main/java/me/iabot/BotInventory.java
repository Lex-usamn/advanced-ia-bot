package me.iabot;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class BotInventory {
    private Map<Material, Integer> items;
    private int maxStackSize = 64;
    private int maxSize = 36;

    public BotInventory() {
        this.items = new HashMap<>();
    }

    public boolean addItem(ItemStack item) {
        Material material = item.getType();
        int amount = item.getAmount();
        
        int currentAmount = items.getOrDefault(material, 0);
        int newAmount = currentAmount + amount;
        
        if (newAmount <= maxSize * maxStackSize) {
            items.put(material, newAmount);
            return true;
        }
        return false;
    }

    public boolean removeItem(Material material, int amount) {
        int currentAmount = items.getOrDefault(material, 0);
        if (currentAmount >= amount) {
            items.put(material, currentAmount - amount);
            if (items.get(material) == 0) {
                items.remove(material);
            }
            return true;
        }
        return false;
    }

    public int getItemCount(Material material) {
        return items.getOrDefault(material, 0);
    }

    public boolean hasItem(Material material, int amount) {
        return getItemCount(material) >= amount;
    }

    public Map<Material, Integer> getItems() {
        return new HashMap<>(items);
    }

    public void clear() {
        items.clear();
    }

    public int getTotalItems() {
        return items.values().stream().mapToInt(Integer::intValue).sum();
    }

    public boolean isFull() {
        return getTotalItems() >= maxSize * maxStackSize;
    }

    @Override
    public String toString() {
        if (items.isEmpty()) {
            return "Vazio";
        }
        
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Material, Integer> entry : items.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(entry.getKey().name()).append(":").append(entry.getValue());
        }
        return sb.toString();
    }
}