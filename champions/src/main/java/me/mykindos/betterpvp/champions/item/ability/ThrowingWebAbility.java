package me.mykindos.betterpvp.champions.item.ability;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableItem;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableListener;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerTypes;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.world.blocks.WorldBlockHandler;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class ThrowingWebAbility extends ItemAbility implements ThrowableListener {

    @EqualsAndHashCode.Include
    private double duration;
    @EqualsAndHashCode.Include
    private double cooldown;
    @EqualsAndHashCode.Include
    private double throwableExpiry;
    private final ChampionsManager championsManager;
    private final WorldBlockHandler blockHandler;
    private final CooldownManager cooldownManager;

    public ThrowingWebAbility(ChampionsManager championsManager, WorldBlockHandler blockHandler, CooldownManager cooldownManager) {
        super(new NamespacedKey(JavaPlugin.getPlugin(Champions.class), "throwing_web"),
                "Throwing Web",
                "Throw a web that temporarily places cobwebs on impact. This can be used to trap enemies.",
                TriggerTypes.LEFT_CLICK);
        this.championsManager = championsManager;
        this.blockHandler = blockHandler;
        this.cooldownManager = cooldownManager;
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        Player player = Objects.requireNonNull(client.getGamer().getPlayer());
        if (!cooldownManager.use(player, getName(), (float) getCooldown(), true, true)) {
            return false;
        }
        // Launch the web
        Item item = player.getWorld().dropItem(player.getEyeLocation(), new ItemStack(Material.COBWEB));
        item.getItemStack().editMeta(meta -> {
            meta.getPersistentDataContainer().set(CoreNamespaceKeys.UUID_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());
        });
        item.setVelocity(player.getLocation().getDirection().multiply(1.8));
        ThrowableItem throwableItem = new ThrowableItem(this, item, player, getName(), (long) (throwableExpiry * 1000L), true);
        throwableItem.setCollideGround(true);
        throwableItem.getImmunes().add(player);
        championsManager.getThrowables().addThrowable(throwableItem);
        return true;
    }

    @Override
    public void onThrowableHit(ThrowableItem throwableItem, LivingEntity thrower, LivingEntity hit) {
        handleWebCollision(throwableItem);
    }

    @Override
    public void onThrowableHitGround(ThrowableItem throwableItem, LivingEntity thrower, org.bukkit.Location location) {
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

}