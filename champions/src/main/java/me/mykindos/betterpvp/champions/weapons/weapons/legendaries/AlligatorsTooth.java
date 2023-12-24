package me.mykindos.betterpvp.champions.weapons.weapons.legendaries;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.weapons.types.ChannelWeapon;
import me.mykindos.betterpvp.champions.weapons.types.InteractWeapon;
import me.mykindos.betterpvp.champions.weapons.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseItemEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Iterator;
import java.util.UUID;

@Singleton
@BPvPListener
public class AlligatorsTooth extends ChannelWeapon implements InteractWeapon, LegendaryWeapon, Listener {

    @Inject
    @Config(path = "weapons.alligators-tooth.energy-per-tick", defaultValue = "1.0")
    private double energyPerTick;

    @Inject
    @Config(path = "weapons.alligators-tooth.initial-energy-cost", defaultValue = "10.0")
    private double initialEnergyCost;

    @Inject
    @Config(path = "weapons.alligators-tooth.base-damage", defaultValue = "8.0")
    private double baseDamage;

    @Inject
    @Config(path = "weapons.alligators-tooth.bonus-damage", defaultValue = "4.0")
    private double bonusDamage;

    @Inject
    @Config(path = "weapons.alligators-tooth.strength", defaultValue = "1.0")
    private double velocityStrength;

    private final EnergyHandler energyHandler;
    private final ClientManager clientManager;

    @Inject
    public AlligatorsTooth(EnergyHandler energyHandler, ClientManager clientManager) {
        super(Material.MUSIC_DISC_MALL, 1,UtilMessage.deserialize("<orange>Alligators Tooth"));
        this.energyHandler = energyHandler;
        this.clientManager = clientManager;
    }

    @Override
    public void activate(Player player) {
        active.add(player.getUniqueId());
    }

    @UpdateEvent
    public void doAlligatorsTooth() {
        final Iterator<UUID> iterator = active.iterator();
        while (iterator.hasNext()) {
            final Player player = Bukkit.getPlayer(iterator.next());
            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }

            final Gamer gamer = clientManager.search().online(player).getGamer();
            if (!gamer.isHoldingRightClick() || player.getInventory().getItemInMainHand().getType() != getMaterial()) {
                iterator.remove();
                continue;
            }

            var checkUsageEvent = UtilServer.callEvent(new PlayerUseItemEvent(player, this, true));
            if (checkUsageEvent.isCancelled()) {
                UtilMessage.simpleMessage(player, "Restriction", "You cannot use this weapon here.");
                continue;
            }

            if (!canUse(player)) {
                iterator.remove();
                continue;
            }

            if (!energyHandler.use(player, "Alligators Tooth", energyPerTick, true)) {
                iterator.remove();
                continue;
            }

            UtilVelocity.velocity(player, velocityStrength, 0.11D, 1.0D, true);
            player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, Material.LAPIS_BLOCK);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FISH_SWIM, 0.8F, 1.5F);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(CustomDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!isHoldingWeapon(player)) return;

        event.setDamage(baseDamage);
        if (event.getDamager().getLocation().getBlock().isLiquid()) {
            event.setDamage(event.getDamage() + bonusDamage);
        }

    }

    @Override
    public boolean canUse(Player player) {
        if (!player.isInWater()) {
            UtilMessage.simpleMessage(player, "Alligators Tooth", "You can only use this weapon in water!");
            return false;
        }
        return true;
    }


    @Override
    public double getEnergy() {
        return initialEnergyCost;
    }

}
