package net.okocraft.chestshopsearcher.command;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.Acrobot.Breeze.Utils.MaterialUtil;
import com.Acrobot.Breeze.Utils.Encoding.Base62;
import com.Acrobot.ChestShop.ChestShop;
import com.Acrobot.ChestShop.Database.DaoCreator;
import com.Acrobot.ChestShop.Database.Item;
import com.Acrobot.ChestShop.Events.ItemParseEvent;
import com.Acrobot.ChestShop.Events.ShopCreatedEvent;
import com.Acrobot.ChestShop.ORMlite.dao.CloseableIterator;
import com.Acrobot.ChestShop.Signs.ChestShopSign;
import com.Acrobot.ChestShop.UUIDs.NameManager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import net.okocraft.chestshopsearcher.database.Shop;
import net.okocraft.chestshopsearcher.database.ShopManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 表示形式:
 * <p>
 * {@code =----- item shops (%item% <- hover text) -----=}
 * <p>
 * {@code player_name: ITEM_NAME(hover here to see item) \@world,0,0,0(click here to tp) B BPrice : SPrice S / 1 item(s)}
 * <p>
 * {@code player_name: ITEM_NAME(no meta items, no hover) \@world,0,0,0 B BPrice / 32 items}
 * <p>
 * {@code player_name: ITEM_NAME \@world,0,0,0 S SPrice / 64 items}
 */
public final class SearchCommand extends BaseCommand implements Listener {

    private static final List<String> ALL_PARAMS = List.of("world", "x", "y", "z", "item", "owner", "stock", "quantity",
            "sell_price", "buy_price");
    private final Map<CommandSender, List<Shop>> previousSearchResult = new HashMap<>();
    @NotNull
    private final List<String> allItems;
    @NotNull
    private final List<String> offlinePlayers;

    @EventHandler
    public void onShopCreate(@NotNull ShopCreatedEvent event) {
        String playerName = event.getPlayer().getName();
        if (!offlinePlayers.contains(playerName) && !playerName.equals("Admin Shop")) {
            offlinePlayers.add(playerName);
        }
        String item = event.getSignLine(ChestShopSign.ITEM_LINE).replaceAll(" ", "_");
        if (!allItems.contains(item)) {
            allItems.add(item);
        }
    }

    @SuppressWarnings("serial")
    private final Set<String> chestShopItemIds = new HashSet<String>() {
        {
            try (CloseableIterator<Item> it = DaoCreator.getDao(Item.class).closeableIterator()) {
                while (it.hasNext()) {
                    add(codeToName(Base62.encode(it.next().getId())));
                }
            } catch (@NotNull SQLException | IOException ignored) {
            }

            for (Material material : Material.values()) {
                add(material.name());
            }
        }
    };

    @EventHandler
    public void onItemParse(@NotNull ItemParseEvent event) {
        if (event.getItem().hasItemMeta()) {
            chestShopItemIds.add(codeToName(MaterialUtil.Metadata.getItemCode(event.getItem())));
        }
    }

    @NotNull
    private String codeToName(@NotNull String code) {
        ItemStack item = ChestShop.getItemDatabase().getFromCode(code);
        if (item == null) {
            return "";
        }

        return item.getType().name() + "#" + code;
    }

    protected SearchCommand() {
        super("chestshopsearcher.search", 1, true, true,
                // /css search column=str column=v1..v2 ...
                "/css search <condition...>");
        
        Set<String> allItems = new HashSet<>();
        Set<String> offlinePlayers = new HashSet<>();
        for (Shop shop : ShopManager.getInstance().getAll()) {
            String ownerName;
            try {
                UUID ownerUniqueId = UUID.fromString(shop.getOwnerUniqueId());
                if (NameManager.isAdminShop(ownerUniqueId)) {
                    ownerName = "Admin_Shop";
                } else {
                    ownerName = Bukkit.getOfflinePlayer(ownerUniqueId).getName();
                }
            } catch (IllegalArgumentException e) {
                ownerName = null;
            }
            if (ownerName != null) {
                offlinePlayers.add(ownerName);
            }
            allItems.add(shop.getItem().replaceAll(" ", "_"));
        }
        this.allItems = new ArrayList<>(allItems);
        this.offlinePlayers = new ArrayList<>(offlinePlayers);
    }

    @Override
    public boolean runCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        // if no condition. (only /css search)
        if (args.length == 1) {
            List<Shop> shops = ShopManager.getInstance().getAll();
            previousSearchResult.put(sender, shops);
            showResult(sender, shops, 1);
            return true;
        }

        try {
            int page = Integer.parseInt(args[1]);
            showResult(sender, previousSearchResult.getOrDefault(sender, List.of()), page);
            return true;
        } catch (NumberFormatException ignored) {
        }
        // if args[2] is not integer.

        Map<String, String> params = parseArgs(args);

        @SuppressWarnings("serial")
        List<String> conditions = new ArrayList<String>() {{
            add(getOwnerCondtion(params.get("owner")));
            add(getItemCondition(params.get("item")));
            add(getValueRangeCondition("buy_price", getDoubleRangeArg(params, "buy_price")));
            add(getValueRangeCondition("sell_price", getDoubleRangeArg(params, "sell_price")));
            add(getValueRangeCondition("stock", getIntRangeArg(params, "stock")));
            add(getValueRangeCondition("quantity", getIntRangeArg(params, "quantity")));
        }};
        
        String where = "";
        if (!conditions.isEmpty()) {
            for (String condition : conditions) {
                where = where + condition;
            }

            where = where.replaceAll("^ and", " where");
        }
        List<Shop> shops = ShopManager.getInstance().select("select c from Shop c" + where);
        if (shops == null) {
            shops = new ArrayList<>();
        }

        // x[0] = min, x[1] = max
        Integer[] x = getIntRangeArg(params, "x");
        Integer[] y = getIntRangeArg(params, "y");
        Integer[] z = getIntRangeArg(params, "z");
        shops.removeIf(shop -> {
            Location shopLocation = ShopManager.fromDBKey(shop.getLocation());
            if (shopLocation.getWorld() == null) {
                return true;
            }

            if (params.containsKey("world")) {
                World world = Bukkit.getWorld(params.get("world"));
                if (world != null && !shopLocation.getWorld().equals(world)) {
                    return true;
                }
            }

            if (x != null && (shopLocation.getBlockX() < x[0] || shopLocation.getBlockX() > x[1])) {
                return true;
            }

            if (y != null && (shopLocation.getBlockY() < y[0] || shopLocation.getBlockY() > y[1])) {
                return true;
            }

            if (z != null && (shopLocation.getBlockZ() < z[0] || shopLocation.getBlockZ() > z[1])) {
                return true;
            }

            return false;
        });

        previousSearchResult.put(sender, shops);

        showResult(sender, shops, 1);
        return true;
    }

    private void showResult(@NotNull CommandSender sender, @NotNull List<Shop> shops, int page) {
        MESSAGES.sendSearchResultHeader(sender);
        if (page <= 0) {
            return;
        }

        for (int i = (page - 1) * 8; i < shops.size() && i < page * 8; i++) {
            MESSAGES.sendSearchResultLine(sender, shops.get(i));
        }

        MESSAGES.sendSpecifyPageToSeeMore(sender);
    }

    @Nullable
    private Integer[] getIntRangeArg(@NotNull Map<String, String> params, String arg) {
        Integer[] result = new Integer[2];
        if (params.containsKey(arg)) {
            String[] split = params.get(arg).split("\\.\\.");
            try {
                result[0] = Integer.parseInt(split[0]);
            } catch (NumberFormatException e) {
                result[0] = null;
            }
            try {
                result[1] = Integer.parseInt(split[1]);
            } catch (NumberFormatException e) {
                result[1] = null;
            }
        } else {
            return null;
        }
        return result;
    }

    @Nullable
    private Double[] getDoubleRangeArg(@NotNull Map<String, String> params, String arg) {
        Double[] result = new Double[2];
        if (params.containsKey(arg)) {
            String[] split = params.get(arg).split("\\.\\.");
            try {
                result[0] = Double.parseDouble(split[0]);
            } catch (NumberFormatException e) {
                result[0] = null;
            }
            try {
                result[1] = Double.parseDouble(split[1]);
            } catch (NumberFormatException e) {
                result[1] = null;
            }
        } else {
            return null;
        }
        return result;
    }

    @NotNull
    @SuppressWarnings("deprecation")
    private String getOwnerCondtion(@Nullable String ownerName) {
        if (ownerName == null || ownerName.isBlank()) {
            return "";
        }

        String ownerUniqueId = Bukkit.getOfflinePlayer(ownerName.equalsIgnoreCase("admin_shop") ? "Admin Shop" : ownerName).getUniqueId().toString();

        return " and c.ownerUniqueId='" + ownerUniqueId + "'";
    }

    @NotNull
    private String getValueRangeCondition(@Nullable String field, @Nullable Number[] range) {
        if (field == null || field.isBlank() || range == null || range.length != 2 || (range[0] == null && range[1] == null)) {
            return "";
        }

        if (range[0] != null && range[1] == null) {
            return " and c." + field + " >= " + range[0];

        } else if (range[0] == null && range[1] != null) {
            return " and c." + field + " <= " + range[1];

        } else if (range[0] != null && range[1] != null) {
            return " and (c." + field + " between " + range[0] + " and " + range[1] + ")";
        }

        return "";
    }

    @NotNull
    private String getItemCondition(@Nullable String item) {
        if (item == null || item.isBlank()) {
            return "";
        }

        ItemStack itemStack = MaterialUtil.getItem(item);
        if (itemStack == null) {
            return "";
        }

        return " and c.item='" + MaterialUtil.getName(itemStack) + "'";
    }

    /**
     * 文字列配列を解析して検索条件のマップを作る。
     * 
     * @param args
     * @return それぞれのカラムをキーに取ったマップ
     */
    @NotNull
    private Map<String, String> parseArgs(@NotNull String[] args) {
        Map<String, String> result = new HashMap<>();
        // args[0]はsearchのはずなのでスキップ
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (ALL_PARAMS.contains(arg) && args.length >= i + 2 && isValidValue(arg, args[i + 1])) {
                if (!result.containsKey(arg)) {
                    result.put(arg, args[i + 1]);
                }

                i++;
            }
        }
        return result;
    }

    private boolean isValidValue(@NotNull String column, @NotNull String value) {
        if (column.equals("world")) {
            return value.matches("[a-zA-Z\\-_0-9]+");
        }
        if (column.equals("item")) {
            try {
                return Material.valueOf(value.split("#")[0]) != Material.AIR;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        if (column.equals("owner")) {
            if (value == null || value.isEmpty()) {
                return false;
            }
            @SuppressWarnings("deprecation")
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(value);
            return offlinePlayer.hasPlayedBefore() && offlinePlayer.getName() != null;
        }
        if (column.equals("sell_price") || column.equals("buy_price")) {
            try {
                String[] split = value.split("\\.\\.");
                return split.length == 2 && Double.parseDouble(split[0]) <= Double.parseDouble(split[1]);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        if (column.equals("stock") || column.equals("quantity") || column.equals("x") || column.equals("y")
                || column.equals("z")) {
            try {
                String[] split = value.split("\\.\\.");
                return split.length == 2 && Integer.parseInt(split[0]) <= Integer.parseInt(split[1]);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        return false;
    }

    @Override
    public List<String> runTabComplete(CommandSender sender, @NotNull String[] args) {
        Map<String, String> params = parseArgs(args);
        String arg = args[args.length - 1];

        List<String> availableParams = new ArrayList<>(ALL_PARAMS);
        availableParams.removeIf(params::containsKey);

        if (availableParams.isEmpty()) {
            return List.of();
        }

        if (!availableParams.contains(args[args.length - 2])) {
            return StringUtil.copyPartialMatches(arg, availableParams, new ArrayList<>());
        }

        String column = args[args.length - 2].toLowerCase(Locale.ROOT);

        if (column.equals("stock")) {
            return StringUtil.copyPartialMatches(arg, List.of("1..100", "10..100", "100..1000"), new ArrayList<>());
        }

        if (column.equals("quantity")) {
            return StringUtil.copyPartialMatches(arg, List.of("1..16", "1..32", "1..64"), new ArrayList<>());
        }

        if (column.equals("sell_price") || column.equals("buy_price")) {
            return StringUtil.copyPartialMatches(arg, List.of("1..1000", "10.0..1000.0"), new ArrayList<>());
        }

        if (column.equals("owner")) {
            return StringUtil.copyPartialMatches(arg, offlinePlayers, new ArrayList<>());
        }

        if (column.equals("item")) {
            return StringUtil.copyPartialMatches(arg, chestShopItemIds, new ArrayList<>());
        }

        if (column.equals("world")) {
            List<String> worlds = Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList());
            return StringUtil.copyPartialMatches(arg, worlds, new ArrayList<>());
        }

        if (column.equals("x") || column.equals("y") || column.equals("z")) {
            return StringUtil.copyPartialMatches(arg, List.of("0..0", "-100..500", "1000..1500"), new ArrayList<>());
        }

        return List.of();
    }
}