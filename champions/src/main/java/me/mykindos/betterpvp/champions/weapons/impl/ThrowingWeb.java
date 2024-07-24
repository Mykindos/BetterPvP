package me.mykindos.betterpvp.champions.weapons.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableItem;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableListener;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.combat.weapon.types.CooldownWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.items.BPvPItem;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.world.blocks.WorldBlockHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

@Singleton
@BPvPListener
public class ThrowingWeb extends Weapon implements Listener, InteractWeapon, CooldownWeapon, ThrowableListener {

    private double duration;
    private double throwableExpiry;
    private final ChampionsManager championsManager;
    private final WorldBlockHandler blockHandler;
    private final CooldownManager cooldownManager;

    @Inject
    public ThrowingWeb(Champions champions, ChampionsManager championsManager, WorldBlockHandler blockHandler, CooldownManager cooldownManager) {
        super(champions, "throwing_web");

        this.championsManager = championsManager;
        this.blockHandler = blockHandler;

        this.cooldownManager = cooldownManager;
    }

    @Override
    public void loadWeapon(BPvPItem item) {
        super.loadWeapon(item);
        ShapedRecipe shapedRecipe = getShapedRecipe("*S*", "SSS", "*S*");
        shapedRecipe.setIngredient('S', Material.STRING);
        Bukkit.addRecipe(shapedRecipe);
    }

    @Override
    public void activate(Player player) {
        Item item = player.getWorld().dropItem(player.getEyeLocation(), new ItemStack(Material.COBWEB));
        item.getItemStack().getItemMeta().getPersistentDataContainer().set(CoreNamespaceKeys.UUID_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());
        item.setVelocity(player.getLocation().getDirection().multiply(1.8));

        ThrowableItem throwableItem = new ThrowableItem(this, item, player, getSimpleName(), (long) (throwableExpiry * 1000L), true);
        throwableItem.setCollideGround(true);
        throwableItem.getImmunes().add(player);
        championsManager.getThrowables().addThrowable(throwableItem);

        UtilInventory.remove(player, Material.COBWEB, 1);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!enabled) {
            return;
        }
        if (event.useItemInHand() != Event.Result.DENY && event.getAction().isLeftClick() && matches(event.getItem())) {
            if (cooldownManager.use(event.getPlayer(), getSimpleName(), getCooldown(), showCooldownFinished(), true, false, x -> isHoldingWeapon(event.getPlayer()), 1001)) {
                activate(event.getPlayer()); // also activate on left click
            }
        }
    }

    @Override
    public void onThrowableHit(ThrowableItem throwableItem, LivingEntity thrower, LivingEntity hit) {
        handleWebCollision(throwableItem);
    }

    @Override
    public void onThrowableHitGround(ThrowableItem throwableItem, LivingEntity thrower, Location location) {
        handleWebCollision(throwableItem);
    }

    private void handleWebCollision(ThrowableItem throwableItem) {
        for (Block block : UtilBlock.getInRadius(throwableItem.getItem().getLocation().getBlock(), 1).keySet()) {
            if (UtilBlock.airFoliage(block)) {
                if (!block.getType().name().contains("GATE") && !block.getType().name().contains("DOOR")) {
                    blockHandler.addRestoreBlock(block, Material.COBWEB, (long) (duration * 1000L));
                    Particle.BLOCK.builder().data(Material.COBWEB.createBlockData())
                            .location(block.getLocation()).count(1).receivers(30).extra(0).spawn();
                }
            }
        }


        throwableItem.getItem().remove();
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
    public boolean showCooldownOnItem() {
        return true;
    }

    @Override
    public void loadWeaponConfig() {
        duration = getConfig("duration", 2.5, Double.class);
        throwableExpiry = getConfig("throwableExpiry", 10.0, Double.class);
    }
}
