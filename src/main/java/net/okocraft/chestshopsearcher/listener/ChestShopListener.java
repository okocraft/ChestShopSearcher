package net.okocraft.chestshopsearcher.listener;

import java.util.Objects;
import java.util.Optional;

import com.Acrobot.Breeze.Utils.MaterialUtil;
import com.Acrobot.Breeze.Utils.PriceUtil;
import com.Acrobot.ChestShop.Database.Account;
import com.Acrobot.ChestShop.Events.ShopCreatedEvent;
import com.Acrobot.ChestShop.Events.ShopDestroyedEvent;
import com.Acrobot.ChestShop.Events.TransactionEvent;
import com.Acrobot.ChestShop.Signs.ChestShopSign;
import com.Acrobot.ChestShop.UUIDs.NameManager;
import com.Acrobot.ChestShop.Utils.uBlock;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Container;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import net.okocraft.chestshopsearcher.Main;
import net.okocraft.chestshopsearcher.database.Shop;
import net.okocraft.chestshopsearcher.database.ShopManager;
import org.jetbrains.annotations.NotNull;

public class ChestShopListener implements Listener {

    private final Main plugin = Main.getInstance();
    @NotNull
    private static ChestShopListener instance = new ChestShopListener();

    private ChestShopListener() {
    }

    @NotNull
    public static ChestShopListener getInstance() {
        return instance;
    }

    public void start() {
        stop();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void stop() {
        HandlerList.unregisterAll(this);
    }

    private Shop createShop(@NotNull Location signLocation, String[] signLines, @NotNull Account ownerAccount) {
        String location = ShopManager.toDBKey(signLocation);
        String owner = ownerAccount.getUuid().toString();
        String item = signLines[ChestShopSign.ITEM_LINE];
        int stock = NameManager.isAdminShop(ownerAccount.getUuid()) ? -1 : getStock(uBlock.findConnectedContainer(signLocation.getBlock()), item);
        String priceLine = signLines[ChestShopSign.PRICE_LINE];
        String buyPrice = PriceUtil.getExactBuyPrice(priceLine).toString();
        String sellPrice = PriceUtil.getExactSellPrice(priceLine).toString();
        int quantity = 1;
        try {
            quantity = Integer.parseInt(signLines[ChestShopSign.QUANTITY_LINE]);
        } catch (NumberFormatException ignore) {
        }

        return new Shop(location, owner, stock, quantity, item, buyPrice, sellPrice);
    }

    private void registerShop(@NotNull Shop shop) {
        Shop registeredShop = ShopManager.getInstance().find(shop.getLocation());
        if (registeredShop == null) {
            ShopManager.getInstance().persist(shop);
        } else {
            ShopManager.getInstance().merge(shop);
        }
    }

    @EventHandler
    public void onShopCreated(@NotNull ShopCreatedEvent event) {
        registerShop(Objects.requireNonNull(createShop(event.getSign().getLocation(), event.getSignLines(), Objects.requireNonNull(event.getOwnerAccount()))));
    }

    @EventHandler
    public void onTransaction(@NotNull TransactionEvent event) {
        registerShop(createShop(event.getSign().getLocation(), event.getSign().getLines(), event.getOwnerAccount()));
    }

    @EventHandler
    public void onShopRemoved(@NotNull ShopDestroyedEvent event) {
        ShopManager shopManager = ShopManager.getInstance();
        Optional.ofNullable(shopManager.find(ShopManager.toDBKey(event.getSign().getLocation())))
                .ifPresent(shop -> shopManager.remove(shop.getLocation()));
    }

    private int getStock(@NotNull Container container, String name) {
        ItemStack item = MaterialUtil.getItem(name);
        int stock = 0;
        for (ItemStack content : container.getInventory()) {
            if (content != null && content.isSimilar(item)) {
                stock += content.getAmount();
            }
        }

        return stock;
    }
}