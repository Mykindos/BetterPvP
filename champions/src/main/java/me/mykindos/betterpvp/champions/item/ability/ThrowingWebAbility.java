package me.mykindos.betterpvp.champions.item.ability;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableItem;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableListener;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.interaction.CooldownInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.world.blocks.WorldBlockHandler;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class ThrowingWebAbility extends CooldownInteraction implements ThrowableListener {

    @EqualsAndHashCode.Include
    private double duration;
    @EqualsAndHashCode.Include
    private double cooldown;
    @EqualsAndHashCode.Include
    private double throwableExpiry;

    private final ChampionsManager championsManager;
    private final WorldBlockHandler blockHandler;

    public ThrowingWebAbility(ChampionsManager championsManager, WorldBlockHandler blockHandler, CooldownManager cooldownManager) {
        super("Throwing Web",
                "Throw a web that temporarily places cobwebs on impact. This can be used to trap enemies.",
                cooldownManager);
        this.championsManager = championsManager;
        this.blockHandler = blockHandler;
    }

    @Override
    public double getCooldown() {
        return cooldown;
    }

    @Override
    protected @NotNull InteractionResult doCooldownExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                            @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        if (!(actor.getEntity() instanceof Player player)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
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
        return InteractionResult.Success.ADVANCE;
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
