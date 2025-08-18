package me.mykindos.betterpvp.champions.item.ability;

import com.google.inject.Inject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerTypes;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.world.blocks.WorldBlockHandler;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class TillingTremorAbility extends ItemAbility {

    private double cooldown;
    private double damage;
    
    @EqualsAndHashCode.Exclude
    private final CooldownManager cooldownManager;
    @EqualsAndHashCode.Exclude
    private final WorldBlockHandler worldBlockHandler;

    @Inject
    private TillingTremorAbility(CooldownManager cooldownManager, WorldBlockHandler worldBlockHandler) {
        super(new NamespacedKey(JavaPlugin.getPlugin(Champions.class), "tilling_tremor"), 
              "Tilling Tremor", 
              "Harvest crops in a small radius. Enemies in the area will be damaged and knocked back.",
              TriggerTypes.RIGHT_CLICK);
        this.cooldownManager = cooldownManager;
        this.worldBlockHandler = worldBlockHandler;
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        Player player = Objects.requireNonNull(client.getGamer().getPlayer());
        
        if (!UtilBlock.isGrounded(player)) {
            UtilMessage.simpleMessage(player, "Rake", "You cannot use <alt>Tilling Tremor</alt> while airborne.");
            return false;
        }
        
        if (!cooldownManager.use(player, getName(), cooldown, true)) {
            return false;
        }

        UtilMessage.simpleMessage(player, "Rake", "You used <green>Tilling Tremor<gray>.");

        Location playerLocation = player.getLocation();
        World world = player.getWorld();
        Location centerBlockLocation = playerLocation.clone().add(0, -0.4, 0);

        int radius = 3;

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

                if (allowedCrops.contains(cropType)) {
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

                for (LivingEntity target : UtilEntity.getNearbyEnemies(player, blockLocation, 1.5)) {
                    UtilDamage.doCustomDamage(new CustomDamageEvent(target, player, null, 
                            EntityDamageEvent.DamageCause.CUSTOM, damage, false, getName()));

                    Vector trajectory = UtilVelocity.getTrajectory2d(player.getLocation().toVector(), target.getLocation().toVector());
                    VelocityData velocityData = new VelocityData(trajectory, 1.0, true, 0, 1.0, 1.0, false);
                    UtilVelocity.velocity(target, player, velocityData);
                }
            }
        }
        return true;
    }
} 