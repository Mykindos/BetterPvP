package me.mykindos.betterpvp.champions.weapons.weapons.legendaries;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.energy.EnergyHandler;
import me.mykindos.betterpvp.champions.weapons.types.ChannelWeapon;
import me.mykindos.betterpvp.champions.weapons.types.InteractWeapon;
import me.mykindos.betterpvp.champions.weapons.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseItemEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

@Singleton
@BPvPListener
public class WindBlade extends ChannelWeapon implements InteractWeapon, LegendaryWeapon, Listener {

    @Inject
    @Config(path = "weapons.wind-blade.energy-per-tick", defaultValue = "1.0")
    private double energyPerTick;

    @Inject
    @Config(path = "weapons.wind-blade.initial-energy-cost", defaultValue = "10.0")
    private double initialEnergyCost;

    @Inject
    @Config(path = "weapons.wind-blade.base-damage", defaultValue = "7.0")
    private double baseDamage;

    @Inject
    @Config(path = "weapons.wind-blade.strength", defaultValue = "0.7")
    private double velocityStrength;


    private final EnergyHandler energyHandler;

    @Inject
    public WindBlade(EnergyHandler energyHandler) {
        super(Material.MUSIC_DISC_MELLOHI, 1, UtilMessage.deserialize("<orange>Wind Blade"));
        this.energyHandler = energyHandler;

    }

    @Override
    public List<Component> getLore() {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Long ago, a race of cloud dwellers", NamedTextColor.WHITE));
        lore.add(Component.text("terrorized the skies. A remnant of", NamedTextColor.WHITE));
        lore.add(Component.text("their tyranny, this airy blade is", NamedTextColor.WHITE));
        lore.add(Component.text("the last surviving memoriam from", NamedTextColor.WHITE));
        lore.add(Component.text("their final battle against the Titans.", NamedTextColor.WHITE));
        lore.add(Component.text(""));
        lore.add(UtilMessage.deserialize("<white>Deals <yellow>%.1f Damage <white>with attack", baseDamage));
        lore.add(UtilMessage.deserialize("<yellow>Right-Click <white>to use <green>Flight"));
        return lore;
    }


    @Override
    public void activate(Player player) {
        active.add(player.getUniqueId());
    }

    @UpdateEvent
    public void doWindBlade() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!active.contains(player.getUniqueId())) continue;

            if (player.getInventory().getItemInMainHand().getType() != Material.MUSIC_DISC_MELLOHI) {
                active.remove(player.getUniqueId());
                continue;
            }

            if (!player.isHandRaised()) {
                active.remove(player.getUniqueId());
                continue;
            }

            var checkUsageEvent = UtilServer.callEvent(new PlayerUseItemEvent(player, this, true));
            if (checkUsageEvent.isCancelled()) {
                UtilMessage.simpleMessage(player, "Restriction", "You cannot use this weapon here.");
                continue;
            }

            if (!canUse(player)) {
                active.remove(player.getUniqueId());
                continue;
            }

            if (!energyHandler.use(player, "Wind Blade", energyPerTick, true)) {
                active.remove(player.getUniqueId());
                continue;
            }

            UtilVelocity.velocity(player, velocityStrength, 0.11, 1.0, true);
            player.getWorld().spawnEntity(player.getLocation(), EntityType.LLAMA_SPIT);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.5F, 1.5F);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(CustomDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (isHoldingWeapon(damager)) {
            event.setDamage(baseDamage);
        }
    }

    @EventHandler
    public void onFall(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (isHoldingWeapon(player)) {
            event.setCancelled(true);
        }
    }

    @Override
    public boolean canUse(Player player) {
        if (UtilBlock.isInLiquid(player)) {
            UtilMessage.simpleMessage(player, "Wind Blade", "You cannot use this weapon while in water!");
            return false;
        }
        return true;
    }


    @Override
    public double getEnergy() {
        return initialEnergyCost;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}
