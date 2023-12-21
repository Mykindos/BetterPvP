package me.mykindos.betterpvp.champions.weapons.weapons;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.weapons.Weapon;
import me.mykindos.betterpvp.champions.weapons.types.CooldownWeapon;
import me.mykindos.betterpvp.champions.weapons.types.InteractWeapon;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableItem;
import me.mykindos.betterpvp.core.combat.throwables.events.ThrowableHitEntityEvent;
import me.mykindos.betterpvp.core.combat.throwables.events.ThrowableHitGroundEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.world.blocks.WorldBlockHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

@Singleton
@BPvPListener
public class ThrowingWeb extends Weapon implements Listener, InteractWeapon, CooldownWeapon {

    @Inject
    @Config(path = "weapons.throwing-web.cooldown", defaultValue = "10.0")
    private double cooldown;

    @Inject
    @Config(path = "weapons.throwing-web.duration", defaultValue = "2.5")
    private double duration;

    @Inject
    @Config(path = "weapons.throwing-web.throwable-expiry", defaultValue = "10.0")
    private double throwableExpiry;

    private final ChampionsManager championsManager;
    private final WorldBlockHandler blockHandler;

    @Inject
    public ThrowingWeb(ChampionsManager championsManager, WorldBlockHandler blockHandler) {
        super(Material.COBWEB, Component.text("Throwing Web", NamedTextColor.LIGHT_PURPLE), "throwing_web");

        this.championsManager = championsManager;
        this.blockHandler = blockHandler;
        newShapedRecipe("*S*", "SSS", "*S*");
        shapedRecipe.setIngredient('*', Material.AIR);
        shapedRecipe.setIngredient('S', Material.STRING);
        Bukkit.addRecipe(shapedRecipe);
    }

    @Override
    public void activate(Player player) {
        Item item = player.getWorld().dropItem(player.getEyeLocation(), new ItemStack(Material.COBWEB));
        item.getItemStack().getItemMeta().getPersistentDataContainer().set(CoreNamespaceKeys.UUID_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());
        item.setVelocity(player.getLocation().getDirection().multiply(1.8));

        ThrowableItem throwableItem = new ThrowableItem(item, player, "Throwing Web", (long) (throwableExpiry * 1000L), true, true);
        throwableItem.setCollideGround(true);
        throwableItem.getImmunes().add(player);
        championsManager.getThrowables().addThrowable(throwableItem);

        UtilInventory.remove(player, Material.COBWEB, 1);
    }

    @EventHandler
    public void onGroundCollide(ThrowableHitGroundEvent event) {
        if (event.getThrowable().getName().equalsIgnoreCase("Throwing Web")) {

            for (Block block : UtilBlock.getInRadius(event.getThrowable().getItem().getLocation().getBlock(), 1).keySet()) {
                if (UtilBlock.airFoliage(block)) {
                    if (!block.getType().name().contains("GATE") && !block.getType().name().contains("DOOR")) {
                        blockHandler.addRestoreBlock(block, Material.COBWEB, (long) (duration * 1000L));
                        Particle.BLOCK_CRACK.builder().data(Material.COBWEB.createBlockData())
                                .location(block.getLocation()).count(1).receivers(30).extra(0).spawn();
                    }
                }
            }


            event.getThrowable().getItem().remove();
        }

    }

    @EventHandler
    public void onCollideEntity(ThrowableHitEntityEvent event) {
        if (event.getThrowable().getName().equalsIgnoreCase("Throwing Web")) {

            for (Block block : UtilBlock.getInRadius(event.getCollision().getLocation().getBlock(), 1).keySet()) {
                if (UtilBlock.airFoliage(block)) {
                    if (!block.getType().name().contains("GATE") && !block.getType().name().contains("DOOR")) {
                        blockHandler.addRestoreBlock(block, Material.COBWEB, (long) (duration * 1000L));
                        Particle.BLOCK_CRACK.builder().data(Material.COBWEB.createBlockData())
                                .location(block.getLocation()).count(1).receivers(30).extra(0).spawn();
                    }
                }
            }

            event.getThrowable().getItem().remove();
        }
    }

    @Override
    public boolean canUse(Player player) {
        return isHoldingWeapon(player);
    }

    @Override
    public double getCooldown() {
        return cooldown;
    }

    @Override
    public Action[] getActions() {
        return new Action[]{Action.LEFT_CLICK_AIR};
    }
}
