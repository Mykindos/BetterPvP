package me.mykindos.betterpvp.shops.shops.shopkeepers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.shops.Shops;
import me.mykindos.betterpvp.shops.shops.shopkeepers.types.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;

import java.util.Objects;
import java.util.UUID;

@Singleton
@Slf4j
public class ShopkeeperManager extends Manager<IShopkeeper> {

    private final Shops shops;

    @Inject
    public ShopkeeperManager(Shops shops) {
        this.shops = shops;
    }

    public void loadShopsFromConfig() {

        objects.values().forEach(shopkeeper -> ((LivingEntity) shopkeeper.getEntity()).remove());
        objects.clear();

        var configSection = shops.getConfig().getConfigurationSection("shopkeepers");
        if(configSection == null) return;

        configSection.getKeys(false).forEach(key -> {
            String type = shops.getConfig().getString("shopkeepers." + key + ".type");
            String rawName = shops.getConfig().getString("shopkeepers." + key + ".name", "");
            Component name = UtilMessage.getMiniMessage(rawName);
            World world = Bukkit.getWorld(Objects.requireNonNull(shops.getConfig().getString("shopkeepers." + key + ".world")));
            if(world == null) {
                log.warn("Could not load shopkeeper {} because the world was null", key);
                return;
            }

            double x = shops.getConfig().getDouble("shopkeepers." + key + ".x");
            double y = shops.getConfig().getDouble("shopkeepers." + key + ".y");
            double z = shops.getConfig().getDouble("shopkeepers." + key + ".z");
            
            if(type == null) return;

            switch (type.toUpperCase()) {
                case "ZOMBIE" -> {
                    var zombieKeeper = new ZombieShopkeeper(new Location(world, x, y, z), name);
                    addObject(zombieKeeper.getUUID(), zombieKeeper);
                }
                case "SKELETON" -> {
                    var skeletonKeeper = new SkeletonShopkeeper(new Location(world, x, y, z), name);
                    addObject(skeletonKeeper.getUUID(), skeletonKeeper);
                }
                case "PARROT" -> {
                    var parrotKeeper = new ParrotShopkeeper(new Location(world, x, y, z), name);
                    addObject(parrotKeeper.getUUID(), parrotKeeper);
                }
                default -> {
                    var villagerKeeper = new VillagerShopkeeper(new Location(world, x, y, z), name);
                    addObject(villagerKeeper.getUUID(), villagerKeeper);
                }
            }
        });
    }

    public void saveShopkeeper(String type, String rawName, Location location) {
        String tag = UUID.randomUUID().toString();
        shops.getConfig().set("shopkeepers." + tag + ".type", type);
        shops.getConfig().set("shopkeepers." + tag + ".name", rawName);
        shops.getConfig().set("shopkeepers." + tag + ".world", location.getWorld().getName());
        shops.getConfig().set("shopkeepers." + tag + ".x", location.getX());
        shops.getConfig().set("shopkeepers." + tag + ".y", location.getY());
        shops.getConfig().set("shopkeepers." + tag + ".z", location.getZ());

        shops.saveConfig();
    }
}
