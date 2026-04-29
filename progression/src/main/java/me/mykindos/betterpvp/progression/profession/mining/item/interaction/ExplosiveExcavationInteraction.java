package me.mykindos.betterpvp.progression.profession.mining.item.interaction;

import lombok.Setter;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.interaction.AbstractInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InputMeta;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.progression.profession.mining.util.MiningDetonation;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Supplier;

public class ExplosiveExcavationInteraction extends AbstractInteraction implements DisplayedInteraction {

    private static final Random RANDOM = new Random();

    @Setter private double triggerChance;
    @Setter private int radius;
    @Setter private double oreChance;
    @Setter private Supplier<Material> oreSupplier = () -> null;

    @SuppressWarnings("unused")
    private final ItemFactory itemFactory;
    private final BlockTagManager blockTagManager;

    public ExplosiveExcavationInteraction(ItemFactory itemFactory, BlockTagManager blockTagManager,
                                          double triggerChance, int radius, double oreChance) {
        super("Explosive Excavation");
        this.itemFactory = itemFactory;
        this.triggerChance = triggerChance;
        this.radius = radius;
        this.oreChance = oreChance;
        this.blockTagManager = blockTagManager;
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor,
                                                    @NotNull InteractionContext context,
                                                    @Nullable ItemInstance itemInstance,
                                                    @Nullable ItemStack itemStack) {
        if (!actor.isPlayer()) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        Player player = (Player) actor.getEntity();

        Block broken = context.get(InputMeta.BROKEN_BLOCK).orElse(null);
        if (broken == null || !UtilBlock.isStoneBased(broken)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        if (RANDOM.nextDouble() >= triggerChance) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        detonate(player, broken.getLocation(), radius, oreChance, oreSupplier);
        return InteractionResult.Success.ADVANCE;
    }

    /**
     * Carves an imperfect sphere and lines its outer ~30% shell with ore (where {@code breakBlock}
     * was permitted). The trigger block at {@code center} is skipped — it was already broken by
     * the {@link org.bukkit.event.block.BlockBreakEvent} that spawned this detonation.
     */
    public void detonate(Player player, Location center, int radius, double oreChance,
                         Supplier<Material> oreSupplier) {
        MiningDetonation.detonate(player, center, radius, oreChance, oreSupplier,
                "progression:explosive_excavation", blockTagManager, true,
                ctx -> ctx.broken() && ctx.distSq() >= ctx.shellThresholdSq());

        Particle.FLASH.builder()
                .count(1)
                .color(Color.AQUA)
                .location(center.toCenterLocation())
                .receivers(60)
                .spawn();
        new SoundEffect(Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, (float) (1 + Math.random()), 0.5f).play(center);
        new SoundEffect(Sound.ENTITY_BREEZE_WIND_BURST, (float) Math.random(), 0.5f).play(center);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Explosive Excavation");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Mining has a chance to carve an underground crater whose walls are lined with ore.");
    }
}
