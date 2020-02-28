package net.okocraft.chestshopsearcher.command;

import java.util.List;

import org.bukkit.command.CommandSender;

import net.okocraft.chestshopsearcher.config.Config;
import org.jetbrains.annotations.NotNull;

public final class ReloadCommand extends BaseCommand {

    protected ReloadCommand() {
        super(
                "chestshopsearcher.reload",
                1,
                true,
                true,
                "/css reload"
        );
    }

    @Override
    public boolean runCommand(@NotNull CommandSender sender, String[] args) {
        Config.getInstance().reloadAllConfigs();
        MESSAGES.sendMessage(sender, "command.reload.success");
        return false;
    }

    @Override
    public List<String> runTabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
} 