package me.mykindos.betterpvp.champions.weapons.impl.legendaries;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.events.PreDamageEvent;
import me.mykindos.betterpvp.core.combat.weapon.types.ChannelWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseItemEvent;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

@Singleton
@BPvPListener
public class AlligatorsTooth extends ChannelWeapon implements InteractWeapon, LegendaryWeapon, Listener {

    private static final String ABILITY_NAME = "Gator Stroke";

    private double bonusDamage;
    private double velocityStrength;
    private double skimmingEnergyMultiplier;

    private final EnergyHandler energyHandler;
    private final ClientManager clientManager;

    @Inject
    public AlligatorsTooth(Champions champions, EnergyHandler energyHandler, ClientManager clientManager) {
        super(champions, "alligators_tooth");
        this.energyHandler = energyHandler;
        this.clientManager = clientManager;
    }

    @Override
    public List<Component> getLore(ItemMeta meta) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("This deadly tooth was stolen from", NamedTextColor.WHITE));
        lore.add(Component.text("a nest of reptilian beasts long ago.", NamedTextColor.WHITE));
        lore.add(Component.text("Legends say that the holder is granted", NamedTextColor.WHITE));
        lore.add(Component.text("the underwater agility of an Alligator.", NamedTextColor.WHITE));
        lore.add(Component.text(""));
        lore.add(UtilMessage.deserialize("<white>Deals <yellow>%.1f</yellow> Damage with attack on land", baseDamage));
        lore.add(UtilMessage.deserialize("<white>Deals <yellow>%.1f</yellow> Damage with attack in water", (baseDamage + bonusDamage)));
        lore.add(UtilMessage.deserialize("<yellow>Right-Click <white>to use <green>%s", ABILITY_NAME));
        return lore;
    }

    @Override
    public void activate(Player player) {
        active.add(player.getUniqueId());
    }

    @UpdateEvent
    public void doAlligatorsTooth() {
        if (!enabled) {
            return;
        }

        active.removeIf(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) return true;

            if (!isHoldingWeapon(player)) {
                activeUsageNotifications.remove(player.getUniqueId());
                return true;
            }

            final Gamer gamer = clientManager.search().online(player).getGamer();
            if (!gamer.isHoldingRightClick()) {
                activeUsageNotifications.remove(player.getUniqueId());
                return true;
            }

            var checkUsageEvent = UtilServer.callEvent(new PlayerUseItemEvent(player, this, true));
            if (checkUsageEvent.isCancelled()) {
                UtilMessage.simpleMessage(player, "Restriction", "You cannot use this weapon here.");
                activeUsageNotifications.remove(player.getUniqueId());
                return true;
            }

            if (!canUse(player)) {
                return false;
            }

            var energyToUse = energyPerTick;
            if(!UtilBlock.isWater(player.getEyeLocation().getBlock().getRelative(BlockFace.DOWN))) {
                energyToUse *= skimmingEnergyMultiplier;
            }

            if (!energyHandler.use(player, ABILITY_NAME, energyToUse, true)) {
                activeUsageNotifications.remove(player.getUniqueId());
                return true;
            }

            VelocityData velocityData = new VelocityData(player.getLocation().getDirection(), velocityStrength, false, 0, 0.11, 1.0, true);
            UtilVelocity.velocity(player, null, velocityData);
            player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, Material.LAPIS_BLOCK);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FISH_SWIM, 0.8F, 1.5F);
            return false;
        });
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
        if (de.getDamager().getLocation().getBlock().isLiquid()) {
            de.setDamage(de.getDamage() + bonusDamage);
        }

    }

    @UpdateEvent(delay = 1000)
    public void onOxygendDrain() {
        if (!enabled) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!UtilBlock.isInWater(player)) continue;
            if (!isHoldingWeapon(player)) continue;
            player.setRemainingAir(player.getMaximumAir());
        }
    }

    @Override
    public boolean canUse(Player player) {
        if (!UtilBlock.isInWater(player)) {
            if (!activeUsageNotifications.contains(player.getUniqueId())) {
                UtilMessage.simpleMessage(player, getSimpleName(), String.format("You cannot use <green>%s <gray>out of water", ABILITY_NAME));
                activeUsageNotifications.add(player.getUniqueId());
            }
            return false;
        }
        activeUsageNotifications.remove(player.getUniqueId());
        return true;
    }


    @Override
    public double getEnergy() {
        return initialEnergyCost;
    }

    @Override
    public void loadWeaponConfig() {
        bonusDamage = getConfig("bonusDamage", 4.0, Double.class);
        velocityStrength = getConfig("velocityStrength", 0.7, Double.class);
        skimmingEnergyMultiplier = getConfig("skimmingEnergyMultiplier", 3.0, Double.class);
    }
}