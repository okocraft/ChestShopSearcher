package net.okocraft.chestshopsearcher.database;

import java.math.BigDecimal;
import java.util.UUID;

import com.Acrobot.Breeze.Utils.MaterialUtil;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public class ShopManager extends DatabaseManager<Shop> {

    private static final ShopManager INSTANCE = new ShopManager();

    private ShopManager() {
        super(Shop.class);
    }

    @NotNull
    public static ShopManager getInstance() {
        return INSTANCE;
    }

    @NotNull
    @Override
    public ShopManager persist(@NotNull final Shop shop) {
        if (validate(shop)) {
            super.persist(shop);
        }
        return this;
    }

    @NotNull
    @Override
    public ShopManager merge(@NotNull final Shop shop) {
        if (validate(shop)) {
            super.merge(shop);
        }
        return this;
    }

    private static boolean validate(@NotNull final Shop shop) {
        if (MaterialUtil.getItem(shop.getItem()) == null) {
            return false;
        }

        if (shop.getStock() != -1 && shop.getStock() < 0) {
            return false;
        }

        try {
            fromDBKey(shop.getLocation());
            UUID.fromString(shop.getOwnerUniqueId());
            BigDecimal buyPrice = new BigDecimal(shop.getBuyPrice());
            BigDecimal sellPrice = new BigDecimal(shop.getSellPrice());
            if (!buyPrice.equals(BigDecimal.ONE.negate()) && buyPrice.compareTo(BigDecimal.ZERO) < 0) {
                return false;
            }
            if (!sellPrice.equals(BigDecimal.ONE.negate()) && sellPrice.compareTo(BigDecimal.ZERO) < 0) {
                return false;
            }
        } catch (@NotNull final IllegalArgumentException e) {
            return false;
        }

        return true;
    }

    @NotNull
    public static String toDBKey(@NotNull final Location location) throws IllegalArgumentException {
        final World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("World cannot be null.");
        }

        return world.getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    @NotNull
    public static Location fromDBKey(@NotNull final String dbKey) throws IllegalArgumentException {
        final String[] locationParts = dbKey.split(",");
        if (locationParts.length != 4) {
            throw new IllegalArgumentException("location parts format must be world,x,y,z.");
        }
        final World dbWorld = Bukkit.getWorld(locationParts[0]);
        if (dbWorld == null) {
            throw new IllegalArgumentException("world in location parts does not exist.");
        }
        final int x = Integer.parseInt(locationParts[1]);
        final int y = Integer.parseInt(locationParts[2]);
        final int z = Integer.parseInt(locationParts[3]);

        return new Location(dbWorld, x, y, z);
    }

}