package net.okocraft.chestshopsearcher.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CSSCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    private static final CSSCommand INSTANCE = new CSSCommand();

    protected CSSCommand() {
        super("", 0, true, true, "/css help");
    }

    private enum SubCommands {
        RELOAD(new ReloadCommand()),
        SEARCH(new SearchCommand());

        private final BaseCommand subCommand;

        SubCommands(BaseCommand subCommand) {
            this.subCommand = subCommand;
        }

        public BaseCommand get() {
            return subCommand;
        }

        public static BaseCommand getByName(String name) throws IllegalArgumentException {
            for (SubCommands subCommand : values()) {
                if (subCommand.get().getName().equalsIgnoreCase(name)) {
                    return subCommand.get();
                }
            }

            throw new IllegalArgumentException("There is no command with the name " + name);
        }

        @NotNull
        public static List<String> getPermittedCommandNames(CommandSender sender) {
            List<String> result = new ArrayList<>();
            for (SubCommands subCommand : values()) {
                if (subCommand.get().hasPermission(sender)) {
                    result.add(subCommand.get().getName().toLowerCase(Locale.ROOT));
                }
            }
            return result;
        }
    }

    public static void init() {
        PluginCommand pluginCommand = Objects.requireNonNull(PLUGIN.getCommand("chestshopsearcher"), "Command is not written in plugin.yml");
        pluginCommand.setExecutor(INSTANCE);
        pluginCommand.setTabCompleter(INSTANCE);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            return this.runCommand(sender, args);
        }

        BaseCommand subCommand;
        try {
            subCommand = SubCommands.getByName(args[0]);
        } catch (IllegalArgumentException e) {
            MESSAGES.sendInvalidArgument(sender, args[0]);
            this.runCommand(sender, args);
            return false;
        }

        if (!subCommand.isConsoleCommand()
                && (sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender)) {
            MESSAGES.sendConsoleSenderCannotUse(sender);
            return false;
        }

        if (!subCommand.isPlayerCommand() && sender instanceof Player) {
            MESSAGES.sendPlayerCannotUse(sender);
            return false;
        }

        if (!subCommand.hasPermission(sender)) {
            MESSAGES.sendNoPermission(sender, subCommand.getPermissionNode());
            return false;
        }

        if (!subCommand.isValidArgsLength(args.length)) {
            MESSAGES.sendNotEnoughArguments(sender);
            MESSAGES.sendUsage(sender, subCommand.getUsage());
            return false;
        }

        return subCommand.runCommand(sender, args);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> permittedCommands = SubCommands.getPermittedCommandNames(sender);
        permittedCommands.add("help");
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], permittedCommands, new ArrayList<>());
        }

        if (!permittedCommands.contains(args[0].toLowerCase(Locale.ROOT))) {
            return List.of();
        }

        return SubCommands.getByName(args[0]).runTabComplete(sender, args);
    }

    /**
     * Show help.
     * 
     * @param sender
     * @param args
     * 
     * @return true
     */
    @Override
    public boolean runCommand(@NotNull CommandSender sender, String[] args) {
        MESSAGES.sendMessage(sender, "command.help.header");
        MESSAGES.sendMessage(sender, false, "command.help.format",
                Map.of("%usage%", getUsage(), "%description%", MESSAGES.getMessage("command.help.description")));
        for (SubCommands subCommand : SubCommands.values()) {
            MESSAGES.sendMessage(sender, false, "command.help.format",
                    Map.of("%usage%", subCommand.get().getUsage(), "%description%", subCommand.get().getDescription()));
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> runTabComplete(CommandSender sender, String[] args) {
        // Not used.
        return null;
    }

    @NotNull
    @Override
    public String getName() {
        return "chestshopsearcher";
    }
}