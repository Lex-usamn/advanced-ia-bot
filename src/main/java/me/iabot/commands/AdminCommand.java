package me.iabot.commands;

import me.iabot.Main;
import me.iabot.BotManager;
import me.iabot.utils.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AdminCommand implements CommandExecutor {
    private Main plugin;

    public AdminCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUso: /aiadmin [reload|stats|config|saveall]");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                return handleReload(sender);
            case "stats":
                return handleStats(sender);
            case "config":
                return handleConfig(sender, args);
            case "saveall":
                return handleSaveAll(sender);
            default:
                sender.sendMessage("§cComando inválido! Use: /aiadmin [reload|stats|config|saveall]");
                return true;
        }
    }

    private boolean handleReload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.getConfigManager().reloadConfig();
        sender.sendMessage("§aConfiguração recarregada com sucesso!");
        return true;
    }

    private boolean handleStats(CommandSender sender) {
        BotManager botManager = plugin.getBotManager();
        sender.sendMessage("§e=== Estatísticas do Sistema ===");
        sender.sendMessage("§7Bots Ativos: §e" + botManager.getBotCount());
        sender.sendMessage("§7Limite de Bots: §e" + plugin.getConfig().getInt("bots.max-bots"));
        sender.sendMessage("§7Aprendizado: " + 
            (plugin.getConfig().getBoolean("bots.enable-learning") ? "§aAtivado" : "§cDesativado"));
        return true;
    }

    private boolean handleConfig(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUso: /aiadmin config [key] [value]");
            return true;
        }

        if (args.length < 3) {
            // Mostrar valor atual
            String key = args[1];
            Object value = plugin.getConfig().get(key, "§cNão encontrado");
            sender.sendMessage("§7" + key + ": §e" + value);
        } else {
            // Definir novo valor
            String key = args[1];
            String value = args[2];
            
            // Converter tipo se necessário
            if (isNumeric(value)) {
                plugin.getConfig().set(key, Double.parseDouble(value));
            } else if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                plugin.getConfig().set(key, Boolean.parseBoolean(value));
            } else {
                plugin.getConfig().set(key, value);
            }
            
            plugin.saveConfig();
            sender.sendMessage("§aConfiguração §e" + key + " §adefinida para §e" + value);
        }

        return true;
    }

    private boolean handleSaveAll(CommandSender sender) {
        plugin.getBotManager().saveAllBots();
        sender.sendMessage("§aTodos os bots salvos com sucesso!");
        return true;
    }

    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}