package me.mykindos.betterpvp.champions.item.ability;

import com.google.inject.Inject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.interaction.CooldownInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.combat.InteractionDamageCause;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.format.NamedTextColor;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.world.blocks.WorldBlockHandler;
import net.kyori.adventure.text.Component;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class TillingTremorAbility extends CooldownInteraction implements DisplayedInteraction {

    private double cooldown;
    private double damage;

    @EqualsAndHashCode.Exclude
    private final WorldBlockHandler worldBlockHandler;

    @Inject
    public TillingTremorAbility(CooldownManager cooldownManager, WorldBlockHandler worldBlockHandler) {
        super("tilling_tremor", cooldownManager);
        this.worldBlockHandler = worldBlockHandler;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Translations.component("champions.ability.tilling-tremor.name");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Translations.component("champions.ability.tilling-tremor.description");
    }

    @Override
    public double getCooldown(InteractionActor actor) {
        return cooldown;
    }

    @Override
    protected @NotNull InteractionResult doCooldownExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                            @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        LivingEntity entity = actor.getEntity();

        // Consume durability
        if (itemStack != null && entity instanceof Player player) {
            UtilItem.damageItem(player, itemStack, 1);
        }

        UtilMessage.message(entity, "core.prefix.rake", "champions.item.tilling-tremor.used", Translations.component("champions.item.tilling-tremor.name").color(NamedTextColor.GREEN));

        Location playerLocation = entity.getLocation();
        World world = entity.getWorld();
        Location centerBlockLocation = playerLocation.clone().add(0, -0.4, 0);

        int radius = 5;
        for (LivingEntity target : UtilEntity.getNearbyEnemies(entity, playerLocation, radius)) {
            UtilDamage.doDamage(new DamageEvent(target, entity, null,
                    new InteractionDamageCause(this), damage, "Tilling Tremor"));

            Vector trajectory = UtilVelocity.getTrajectory2d(entity.getLocation().toVector(), target.getLocation().toVector());
            VelocityData velocityData = new VelocityData(trajectory, 2.0, true, 0, 1.0, 1.0, false);
            UtilVelocity.velocity(target, entity, velocityData);
        }

        BlockData belowMaterial = centerBlockLocation.getBlock().getType().isAir()
                ? Material.GRASS_BLOCK.createBlockData()
                : centerBlockLocation.getBlock().getBlockData();
        Particle.BLOCK.builder()
                .data(belowMaterial)
                .location(playerLocation)
                .offset(radius, radius, radius)
                .count(100)
                .receivers(60)
                .spawn();
        new SoundEffect(Sound.ENTITY_ZOMBIE_INFECT, 1f, 1f).play(playerLocation);

        final Set<Material> allowedCrops = EnumSet.of(
                Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
                Material.NETHER_WART, Material.SWEET_BERRY_BUSH, Material.MELON, Material.PUMPKIN,
                Material.SUGAR_CANE, Material.BAMBOO
        );

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Location blockLocation = centerBlockLocation.clone().add(x, 0, z);
                Block block = world.getBlockAt(blockLocation);

                if(worldBlockHandler.isRestoreBlock(block)) {
                    continue;
                }

                world.playEffect(blockLocation, Effect.STEP_SOUND, block.getType());

                Block cropBlock = world.getBlockAt(blockLocation.clone().add(0, 1, 0));
                Material cropType = cropBlock.getType();

                if (allowedCrops.contains(cropType) && entity instanceof Player player) {
                    if (cropBlock.getBlockData() instanceof Ageable crop && cropType != Material.SWEET_BERRY_BUSH) {
                        if (crop.getAge() == crop.getMaximumAge()) {
                            Collection<ItemStack> drops = cropBlock.getDrops();
                            for (ItemStack drop : drops) {
                                UtilItem.insert(player, drop);
                            }

                            crop.setAge(0);
                            cropBlock.setBlockData(crop);
                            continue;
                        }
                    }

                    if (cropType == Material.MELON || cropType == Material.PUMPKIN) {
                        Collection<ItemStack> drops = cropBlock.getDrops();
                        for (ItemStack drop : drops) {
                            UtilItem.insert(player, drop);
                        }
                        cropBlock.setType(Material.AIR);
                        continue;
                    }

                    if (cropType == Material.SWEET_BERRY_BUSH && cropBlock.getBlockData() instanceof Ageable berryBush) {
                        if (berryBush.getAge() >= berryBush.getMaximumAge() - 1) {
                            Collection<ItemStack> drops = cropBlock.getDrops();
                            for (ItemStack drop : drops) {
                                UtilItem.insert(player, drop);
                            }

                            berryBush.setAge(1);
                            cropBlock.setBlockData(berryBush);
                            continue;
                        }
                    }

                    if (cropType == Material.SUGAR_CANE || cropType == Material.BAMBOO) {
                        Block blockAbove = world.getBlockAt(cropBlock.getLocation().add(0, 1, 0));
                        if (blockAbove.getType() == Material.SUGAR_CANE || blockAbove.getType() == Material.BAMBOO) {
                            Collection<ItemStack> drops = blockAbove.getDrops();
                            for (ItemStack drop : drops) {
                                UtilItem.insert(player, drop);
                            }
                            blockAbove.setType(Material.AIR);
                            continue;
                        }
                    }
                }
            }
        }
        return InteractionResult.Success.ADVANCE;
    }
}
