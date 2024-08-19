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
import org.bukkit.Particle;
import org.bukkit.Sound;
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
public class WindBlade extends ChannelWeapon implements InteractWeapon, LegendaryWeapon, Listener {

    private static final String ABILITY_NAME = "Flight";

    private double velocityStrength;
    private final EnergyHandler energyHandler;
    private final ClientManager clientManager;

    @Inject
    public WindBlade(Champions champions, EnergyHandler energyHandler, ClientManager clientManager) {
        super(champions, "wind_blade");
        this.energyHandler = energyHandler;
        this.clientManager = clientManager;
    }

    @Override
    public List<Component> getLore(ItemMeta itemMeta) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Long ago, a race of cloud dwellers", NamedTextColor.WHITE));
        lore.add(Component.text("terrorized the skies. A remnant of", NamedTextColor.WHITE));
        lore.add(Component.text("their tyranny, this airy blade is", NamedTextColor.WHITE));
        lore.add(Component.text("the last surviving memoriam from", NamedTextColor.WHITE));
        lore.add(Component.text("their final battle against the Titans.", NamedTextColor.WHITE));
        lore.add(Component.text(""));
        lore.add(UtilMessage.deserialize("<white>Deals <yellow>%.1f Damage <white>with attack", baseDamage));
        lore.add(UtilMessage.deserialize("<yellow>Right-Click <white>to use <green>%s", ABILITY_NAME));
        return lore;
    }

    @Override
    public void activate(Player player) {
        active.add(player.getUniqueId());
    }

    @UpdateEvent (priority = 100)
    public void doWindBlade() {
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

            if (!energyHandler.use(player, ABILITY_NAME, energyPerTick, true)) {
                activeUsageNotifications.remove(player.getUniqueId());
                return true;
            }

            VelocityData velocityData = new VelocityData(player.getLocation().getDirection(), velocityStrength, false, 0, 0, 1, false);
            UtilVelocity.velocity(player, null, velocityData);
            Particle.POOF.builder().location(player.getLocation()).extra(0).count(3).spawn();
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.2F, 1.5F);
            return false;
        });

    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(PreDamageEvent event) {
        if (!enabled) {
            return;
        }

        DamageEvent cde = event.getDamageEvent();
        if (cde.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(cde.getDamager() instanceof Player damager)) return;
        if (isHoldingWeapon(damager)) {
            cde.setDamage(baseDamage);
            cde.setRawDamage(baseDamage);
        }
    }

    @EventHandler
    public void onFall(EntityDamageEvent event) {
        if (!enabled) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (isHoldingWeapon(player)) {
            event.setCancelled(true);
        }
    }

    @Override
    public boolean canUse(Player player) {
        if (UtilBlock.isInLiquid(player)) {
            if (!activeUsageNotifications.contains(player.getUniqueId())) {
                UtilMessage.simpleMessage(player, getSimpleName(), String.format("You cannot use <green>%s <gray>while in water", ABILITY_NAME));
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
        velocityStrength = getConfig("velocityStrength", 0.5, Double.class);
    }
}