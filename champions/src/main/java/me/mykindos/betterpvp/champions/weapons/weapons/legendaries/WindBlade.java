package me.mykindos.betterpvp.champions.weapons.weapons.legendaries;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.combat.events.PlayerCheckShieldEvent;
import me.mykindos.betterpvp.champions.energy.EnergyHandler;
import me.mykindos.betterpvp.champions.weapons.types.ChannelWeapon;
import me.mykindos.betterpvp.champions.weapons.types.InteractWeapon;
import me.mykindos.betterpvp.champions.weapons.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseItemEvent;
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
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

@Singleton
@BPvPListener
public class WindBlade extends ChannelWeapon implements InteractWeapon, LegendaryWeapon, Listener {

    private final EnergyHandler energyHandler;

    @Inject
    public WindBlade(EnergyHandler energyHandler) {
        super(Material.MUSIC_DISC_MELLOHI, Component.text("Wind Blade", NamedTextColor.RED));
        this.energyHandler = energyHandler;
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
            if(checkUsageEvent.isCancelled()) {
                UtilMessage.simpleMessage(player, "You cannot use this weapon here.");
                continue;
            }

            if (!canUse(player)) {
                active.remove(player.getUniqueId());
                continue;
            }

            if (!energyHandler.use(player, "Wind Blade", 1.0, true)) {
                active.remove(player.getUniqueId());
                continue;
            }

            UtilVelocity.velocity(player, 0.7, 0.11, 1.0, true);
            player.getWorld().spawnEntity(player.getLocation(), EntityType.LLAMA_SPIT);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.5F, 1.5F);
        }
    }

    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            if (event.getDamager() instanceof Player player) {
                if (player.getInventory().getItemInMainHand().getType() != Material.MUSIC_DISC_MELLOHI) return;

                event.setKnockback(false);
                event.setDamage(7D);
                Vector vec = player.getLocation().getDirection();
                vec.setY(0);
                UtilVelocity.velocity(event.getDamagee(), vec, 2D, false, 0.0D, 0.5D, 1.0D, true);
            }
        }
    }

    @EventHandler
    public void onCheckShield(PlayerCheckShieldEvent event) {
        if (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.MUSIC_DISC_MELLOHI) {
            event.setShouldHaveShield(true);
        }
    }

    @EventHandler
    public void onFall(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                if (player.getInventory().getItemInMainHand().getType() != Material.MUSIC_DISC_MELLOHI) return;
                event.setCancelled(true);
            }
        }
    }

    @Override
    public boolean canUse(Player player) {
        if (UtilBlock.isInLiquid(player)) {
            UtilMessage.simpleMessage(player, "You cannot use this weapon while in water!");
            return false;
        }
        return true;
    }


    @Override
    public float getEnergy() {
        return 10;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}
