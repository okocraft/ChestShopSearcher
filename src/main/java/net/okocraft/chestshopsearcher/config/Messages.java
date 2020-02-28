package net.okocraft.chestshopsearcher.config;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import com.Acrobot.Breeze.Utils.MaterialUtil;
import com.Acrobot.ChestShop.UUIDs.NameManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

import net.okocraft.configurationapi.BaseConfig;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.okocraft.chestshopsearcher.Main;
import net.okocraft.chestshopsearcher.ReflectionUtil;
import net.okocraft.chestshopsearcher.database.Shop;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Messages extends BaseConfig {

    private static Main plugin = Main.getInstance();
    @NotNull
    private static Messages instance = new Messages();

    private Messages() {
        super("messages.yml", plugin.getDataFolder(), plugin.getResource("messages.yml"));
    }

    @NotNull
    public static Messages getInstance() {
        return instance;
    }

    /**
     * Send message compoent to player.
     * 
     * @param sender
     * @param addPrefix
     * @param path
     * @param placeholders
     */
    public void sendMessageComponent(@NotNull CommandSender sender, boolean addPrefix, String path,
                                     @NotNull Map<String, BaseComponent> placeholders) {
        String prefix = addPrefix ? getConfig().getString("command.general.info.plugin-prefix", "&8[&6CSS&8]&r") + " "
                : "";
        TextComponent message = new TextComponent();
        String rawMessage = ChatColor.translateAlternateColorCodes('&', prefix + getMessage(path));
        while (true) {
            int placeholderIndexFirst = rawMessage.indexOf("%");
            if (placeholderIndexFirst == -1) {
                message.addExtra(rawMessage);
                break;
            }
            message.addExtra(rawMessage.substring(0, placeholderIndexFirst));
            rawMessage = rawMessage.substring(placeholderIndexFirst + 1);
            int placeholderIndexSecond = rawMessage.indexOf("%");
            String key = "%" + rawMessage.substring(0, placeholderIndexSecond + 1);
            message.addExtra(placeholders.getOrDefault(key, new TextComponent(key)));
            rawMessage = rawMessage.substring(placeholderIndexSecond + 1);
        }

        sender.spigot().sendMessage(message);
    }

    /**
     * Send message to player.
     * 
     * @param sender
     * @param addPrefix
     * @param path
     * @param placeholders
     */
    public void sendMessage(@NotNull CommandSender sender, boolean addPrefix, String path, @NotNull Map<String, String> placeholders) {
        String prefix = addPrefix ? getConfig().getString("command.general.info.plugin-prefix", "&8[&6CSS&8]&r") + " "
                : "";
        String message = prefix + getMessage(path);
        for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
            message = message.replace(placeholder.getKey(), placeholder.getValue());
        }
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    /**
     * Send message to player.
     * 
     * @param sender
     * @param path
     * @param placeholders
     */
    public void sendMessage(@NotNull CommandSender sender, String path, @NotNull Map<String, String> placeholders) {
        sendMessage(sender, true, path, placeholders);
    }

    /**
     * Send message to player.
     * 
     * @param sender
     * @param path
     */
    public void sendMessage(@NotNull CommandSender sender, String path) {
        sendMessage(sender, path, Map.of());
    }

    /**
     * Send message to player.
     * 
     * @param sender
     * @param addPrefix
     * @param path
     */
    public void sendMessage(@NotNull CommandSender sender, boolean addPrefix, String path) {
        sendMessage(sender, addPrefix, path, Map.of());
    }

    /**
     * Gets message from key. Returned messages will not translated its color code.
     * 
     * @param path
     * @return
     */
    public String getMessage(String path) {
        return getConfig().getString(path, path);
    }

    public void sendSearchResultHeader(@NotNull CommandSender sender) {
        sendMessage(sender, "command.search.header");
    }

    public void sendSearchResultLine(@NotNull CommandSender sender, @NotNull Shop shop) {
        String playerName = "%player-name%";
        try {
            UUID uuid = UUID.fromString(shop.getOwnerUniqueId());
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            if (NameManager.isAdminShop(uuid)) {
                playerName = "Admin Shop";
            } else if (player.getName() != null) {
                playerName = player.getName();
            } else {
                playerName = "null";
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        boolean isBuy = !shop.getBuyPrice().equals("-1");
        boolean isSell = !shop.getSellPrice().equals("-1");
        if (isBuy && isSell) {
            sendMessageComponent(sender, false, "command.search.line-buy-and-sell",
                    Map.of("%player-name%", new TextComponent(playerName), "%item%",
                            toTextComponent(MaterialUtil.getItem(shop.getItem())),
                            "%location%", new TextComponent(shop.getLocation()), "%buy-price%",
                            new TextComponent(shop.getBuyPrice()), "%sell-price%",
                            new TextComponent(shop.getSellPrice()), "%quantity%",
                            new TextComponent(String.valueOf(shop.getQuantity()))));
        } else if (isBuy) {
            sendMessageComponent(sender, false, "command.search.line-buy-and-sell",
                    Map.of("%player-name%", new TextComponent(playerName), "%item%",
                            toTextComponent(MaterialUtil.getItem(shop.getItem())),
                            "%location%", new TextComponent(shop.getLocation()), "%buy-price%",
                            new TextComponent(shop.getBuyPrice()), "%quantity%",
                            new TextComponent(String.valueOf(shop.getQuantity()))));
        } else if (isSell) {
            sendMessageComponent(sender, false, "command.search.line-buy-and-sell",
                    Map.of("%player-name%", new TextComponent(playerName), "%item%",
                            toTextComponent(MaterialUtil.getItem(shop.getItem())),
                            "%location%", new TextComponent(shop.getLocation()), "%sell-price%",
                            new TextComponent(shop.getSellPrice()), "%quantity%",
                            new TextComponent(String.valueOf(shop.getQuantity()))));
        }
    }

    public void sendSpecifyPageToSeeMore(@NotNull CommandSender sender) {
        sendMessage(sender, "command.search.specify-page-to-see-more");
    }

    public void sendNoWorld(@NotNull CommandSender sender) {
        sendMessage(sender, "command.search.no-world");
    }

    public void sendInvalidArgument(@NotNull CommandSender sender, @NotNull String invalid) {
        sendMessage(sender, "command.general.error.invalid-argument", Map.of("%argument%", invalid));
    }

    public void sendNoPermission(@NotNull CommandSender sender, @NotNull String permission) {
        sendMessage(sender, "command.general.error.no-permission", Map.of("%permission%", permission));
    }

    public void sendConsoleSenderCannotUse(@NotNull CommandSender sender) {
        sendMessage(sender, "command.general.error.cannot-use-from-console");
    }

    public void sendPlayerCannotUse(@NotNull CommandSender sender) {
        sendMessage(sender, "command.general.error.player-cannot-use");
    }

    public void sendNotEnoughArguments(@NotNull CommandSender sender) {
        sendMessage(sender, "command.general.error.not-enough-arguments");
    }

    public void sendInvalidNumber(@NotNull CommandSender sender, @NotNull String number) {
        sendMessage(sender, "command.general.error.invalid-number", Map.of("%number%", number));
    }

    public void sendUsage(@NotNull CommandSender sender, @NotNull String usage) {
        sendMessage(sender, "command.general.info.usage", Map.of("%usage%", usage));
    }

    public void sendNoPlayerFound(@NotNull CommandSender sender, @NotNull String player) {
        sendMessage(sender, "command.general.error.no-player-found", Map.of("%player%", player));
    }

    public void sendNoPointNameFound(@NotNull CommandSender sender, @NotNull String pointName) {
        sendMessage(sender, "command.general.error.no-point-name-found", Map.of("%point-name%", pointName));
    }

    void reload() {
        reloadConfig();
    }

    /**
     * Converts an {@link org.bukkit.inventory.ItemStack} to a Json string for
     * sending with {@link net.md_5.bungee.api.chat.BaseComponent}'s.
     *
     * @param itemStack the item to convert
     * @return the hover evnet that show the item
     */
    @Nullable
    private TextComponent toTextComponent(@NotNull ItemStack itemStack) {
        // ItemStack methods to get a net.minecraft.server.ItemStack object for
        // serialization
        Class<?> craftItemStackClazz = ReflectionUtil.getOBCClass("inventory.CraftItemStack");
        Method asNMSCopyMethod = ReflectionUtil.getMethod(craftItemStackClazz, "asNMSCopy", ItemStack.class);

        // NMS Method to serialize a net.minecraft.server.ItemStack to a valid Json
        // string
        Class<?> nmsItemStackClazz = ReflectionUtil.getNMSClass("ItemStack");
        Class<?> nbtTagCompoundClazz = ReflectionUtil.getNMSClass("NBTTagCompound");
        Method saveNmsItemStackMethod = ReflectionUtil.getMethod(nmsItemStackClazz, "save", nbtTagCompoundClazz);

        // This will just be an empty NBTTagCompound instance to invoke the saveNms
        // method
        Object nmsNbtTagCompoundObj;

        // This is the net.minecraft.server.ItemStack object received from the asNMSCopy
        // method
        Object nmsItemStackObj;

        // This is the net.minecraft.server.ItemStack after being put through
        // saveNmsItem method
        Object itemAsJsonObject;

        try {
            nmsNbtTagCompoundObj = nbtTagCompoundClazz.getDeclaredConstructor().newInstance();
            nmsItemStackObj = asNMSCopyMethod.invoke(null, itemStack);
            itemAsJsonObject = saveNmsItemStackMethod.invoke(nmsItemStackObj, nmsNbtTagCompoundObj);
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "failed to serialize itemstack to nms item", t);
            return null;
        }
        
        BaseComponent[] hoverEventComponents = new BaseComponent[]{
            new TextComponent(itemAsJsonObject.toString())
        };

        HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_ITEM, hoverEventComponents);
        TextComponent text = new TextComponent(MaterialUtil.getName(itemStack));
        text.setHoverEvent(hover);
        return text;
    }
}