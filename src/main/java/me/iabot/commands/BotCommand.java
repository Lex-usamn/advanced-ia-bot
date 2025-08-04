package me.iabot.commands;

import me.iabot.Main;
import me.iabot.BotAI;
import me.iabot.BotManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BotCommand implements CommandExecutor {
    private Main plugin;

    public BotCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUso: /bot [create|remove|list|info|tp] [nome]");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        BotManager botManager = plugin.getBotManager();

        switch (subCommand) {
            case "create":
                return handleCreate(sender, args, botManager);
            case "remove":
                return handleRemove(sender, args, botManager);
            case "list":
                return handleList(sender, botManager);
            case "info":
                return handleInfo(sender, args, botManager);
            case "tp":
                return handleTeleport(sender, args, botManager);
            default:
                sender.sendMessage("§cComando inválido! Use: /bot [create|remove|list|info|tp] [nome]");
                return true;
        }
    }

    private boolean handleCreate(CommandSender sender, String[] args, BotManager botManager) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem criar bots!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUso: /bot create <nome>");
            return true;
        }

        Player player = (Player) sender;
        String botName = args[1];

        if (botManager.createBot(botName, player.getLocation())) {
            sender.sendMessage("§aBot §e" + botName + " §acriado com sucesso!");
        } else {
            sender.sendMessage("§cFalha ao criar bot! Nome já existe ou limite atingido.");
        }

        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args, BotManager botManager) {
        if (args.length < 2) {
            sender.sendMessage("§cUso: /bot remove <nome>");
            return true;
        }

        String botName = args[1];
        if (botManager.removeBot(botName)) {
            sender.sendMessage("§aBot §e" + botName + " §aremovido com sucesso!");
        } else {
            sender.sendMessage("§cBot §e" + botName + " §cnão encontrado!");
        }

        return true;
    }

    private boolean handleList(CommandSender sender, BotManager botManager) {
        sender.sendMessage("§e=== Bots IA (" + botManager.getBotCount() + "/" + 
            plugin.getConfig().getInt("bots.max-bots") + ") ===");
        
        for (BotAI bot : botManager.getAllBots()) {
            sender.sendMessage("§7- §e" + bot.getName() + " §8(Level " + bot.getLevel() + 
                ") - " + (bot.isOnline() ? "§aOnline" : "§cOffline") + 
                " §7[" + bot.getCurrentTask() + "]");
        }

        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args, BotManager botManager) {
        if (args.length < 2) {
            sender.sendMessage("§cUso: /bot info <nome>");
            return true;
        }

        String botName = args[1];
        BotAI bot = botManager.getBot(botName);

        if (bot == null) {
            sender.sendMessage("§cBot §e" + botName + " §cnão encontrado!");
            return true;
        }

        sender.sendMessage("§e=== Informações do Bot: " + botName + " ===");
        sender.sendMessage("§7Nível: §e" + bot.getLevel());
        sender.sendMessage("§7Experiência: §e" + bot.getExperience() + "/" + (bot.getLevel() * 100));
        sender.sendMessage("§7Tarefa Atual: §e" + bot.getCurrentTask());
        sender.sendMessage("§7Status: " + (bot.isOnline() ? "§aOnline" : "§cOffline"));
        sender.sendMessage("§7Localização: §e" + String.format("X:%.1f Y:%.1f Z:%.1f", 
            bot.getLocation().getX(), bot.getLocation().getY(), bot.getLocation().getZ()));

        return true;
    }

    private boolean handleTeleport(CommandSender sender, String[] args, BotManager botManager) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem teleportar!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUso: /bot tp <nome>");
            return true;
        }

        Player player = (Player) sender;
        String botName = args[1];
        BotAI bot = botManager.getBot(botName);

        if (bot == null) {
            sender.sendMessage("§cBot §e" + botName + " §cnão encontrado!");
            return true;
        }

        player.teleport(bot.getLocation());
        sender.sendMessage("§aTeleportado para o bot §e" + botName);

        return true;
    }
}