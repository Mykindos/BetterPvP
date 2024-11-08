package me.mykindos.betterpvp.champions.weapons.impl.legendaries;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.events.PreDamageEvent;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Singleton
@BPvPListener
public class Rake extends Weapon implements InteractWeapon, LegendaryWeapon, Listener {

    private double rakeCooldown;
    private double damage;
    private final CooldownManager cooldownManager;

    @Inject
    public Rake(Champions champions, CooldownManager cooldownManager) {
        super(champions, "rake");
        this.cooldownManager = cooldownManager;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(PreDamageEvent event) {
        if (!enabled) {
            return;
        }

        DamageEvent de = event.getDamageEvent();
        if (de.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(de.getDamager() instanceof Player player)) return;
        if (!isHoldingWeapon(player)) return;

        de.setDamage(baseDamage);
    }

    @Override
    public List<Component> getLore(ItemMeta meta) {
        List<Component> description = new ArrayList<>();
        description.add(Component.text("Forged in the evangelical farmlands of the Kindos empire,", NamedTextColor.WHITE));
        description.add(Component.text("and tempered in the eternal winds of Tempest Peak,", NamedTextColor.WHITE));
        description.add(Component.text("the Rake is no mere farming tool.", NamedTextColor.WHITE));
        description.add(Component.text(""));
        description.add(Component.text("It is said that the first harvest this Rake performed", NamedTextColor.WHITE));
        description.add(Component.text("was on the battlefield, reaping the souls of a thousand warriors.", NamedTextColor.WHITE));
        description.add(Component.text("The ancient druids, recognizing its power,", NamedTextColor.WHITE));
        description.add(Component.text("infused it with the essence of the earth itself.", NamedTextColor.WHITE));
        description.add(Component.text(""));
        description.add(Component.text("Wielding the Rake is not just an honor, but a responsibility,", NamedTextColor.WHITE));
        description.add(Component.text("for it is whispered that the Rake hungers for the life force", NamedTextColor.WHITE));
        description.add(Component.text("of the earth and the blood of those who oppose its master.", NamedTextColor.WHITE));
        description.add(Component.text(""));

        description.add(UtilMessage.deserialize("<white>Deals <yellow>%.1f Damage <white>per hit", baseDamage));

        description.add(Component.text(""));
        description.add(UtilMessage.deserialize("<yellow>Right-Click <white>to use <green>Tilling Terror"));

        return description;
    }

    @Override
    public void activate(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!item.hasItemMeta()) return;

        if (!cooldownManager.use(player, "Tilling Tremor", rakeCooldown, true)) return;

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

                world.playEffect(blockLocation, Effect.STEP_SOUND, block.getType());

                Block cropBlock = world.getBlockAt(blockLocation.clone().add(0, 1, 0));
                Material cropType = cropBlock.getType();

                if (allowedCrops.contains(cropType)) {
                    if (cropBlock.getBlockData() instanceof Ageable crop && cropType != Material.SWEET_BERRY_BUSH) {
                        if (crop.getAge() == crop.getMaximumAge()) {
                            Collection<ItemStack> drops = cropBlock.getDrops();
                            for (ItemStack drop : drops) {
                                world.dropItemNaturally(cropBlock.getLocation(), drop);
                            }

                            crop.setAge(0);
                            cropBlock.setBlockData(crop);
                            continue;
                        }
                    }

                    if (cropType == Material.MELON || cropType == Material.PUMPKIN) {
                        Collection<ItemStack> drops = cropBlock.getDrops();
                        for (ItemStack drop : drops) {
                            world.dropItemNaturally(cropBlock.getLocation(), drop);
                        }
                        cropBlock.setType(Material.AIR);
                        continue;
                    }

                    if (cropType == Material.SWEET_BERRY_BUSH && cropBlock.getBlockData() instanceof Ageable berryBush) {
                        if (berryBush.getAge() >= berryBush.getMaximumAge() - 1) {
                            Collection<ItemStack> drops = cropBlock.getDrops();
                            for (ItemStack drop : drops) {
                                world.dropItemNaturally(cropBlock.getLocation(), drop);
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
                                world.dropItemNaturally(blockAbove.getLocation(), drop);
                            }
                            blockAbove.setType(Material.AIR);
                            continue;
                        }
                    }
                }

                for (LivingEntity target : UtilEntity.getNearbyEnemies(player, blockLocation, 1.5)) {
                    UtilDamage.doCustomDamage(new CustomDamageEvent(target, player, null, EntityDamageEvent.DamageCause.CUSTOM, damage, false, "Tilling Tremor"));

                    Vector trajectory = UtilVelocity.getTrajectory2d(player.getLocation().toVector(), target.getLocation().toVector());
                    VelocityData velocityData = new VelocityData(trajectory, 1.0, true, 0, 1.0, 1.0, false);
                    UtilVelocity.velocity(target, player, velocityData);
                }
            }
        }
    }


    @Override
    public boolean canUse(Player player) {
        if (!UtilBlock.isGrounded(player)) {
            UtilMessage.simpleMessage(player, "Rake", "You cannot use <alt>Tilling Tremor</alt> while airborne.");
            return false;
        }

        return true;
    }

    @Override
    public void loadWeaponConfig() {
        rakeCooldown = getConfig("rakeCooldown", 3.0, Double.class);
        damage = getConfig("damage", 5.0, Double.class);
    }
}
