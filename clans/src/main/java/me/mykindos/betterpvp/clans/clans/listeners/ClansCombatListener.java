package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.combat.damage.ItemDamageManager;
import me.mykindos.betterpvp.clans.combat.damage.ItemDamageValue;
import me.mykindos.betterpvp.clans.gamer.Gamer;
import me.mykindos.betterpvp.clans.gamer.GamerManager;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.PreCustomDamageEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Optional;

@BPvPListener
public class ClansCombatListener implements Listener {

    private final ClanManager clanManager;
    private final GamerManager gamerManager;
    private final ItemDamageManager itemDamageManager;

    @Inject
    public ClansCombatListener(ClanManager clanManager, GamerManager gamerManager, ItemDamageManager itemDamageManager) {
        this.clanManager = clanManager;
        this.gamerManager = gamerManager;
        this.itemDamageManager = itemDamageManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreDamageSafezone(PreCustomDamageEvent event) {
        if (event.isCancelled()) return;

        CustomDamageEvent cde = event.getCustomDamageEvent();
        if (cde.getDamager() instanceof Player damager && cde.getDamagee() instanceof Player damagee) {
            if (!clanManager.canHurt(damager, damagee)) {
                event.setCancelled(true);
                return;
            }
        }

        LivingEntity damagee = cde.getDamagee();
        Optional<Clan> locationClanOptional = clanManager.getClanByLocation(damagee.getLocation());
        if (locationClanOptional.isPresent()) {
            Clan locationClan = locationClanOptional.get();
            if (locationClan.isAdmin() && locationClan.isSafe()) {
                if (damagee instanceof Player player) {
                    Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId());
                    gamerOptional.ifPresent(gamer -> {
                        if (UtilTime.elapsed(gamer.getLastDamaged(), 15000)) {
                            event.setCancelled(true);
                        }
                    });
                } else {
                    event.setCancelled(true);
                }
            }
        }

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onWeaponDamage(CustomDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player player)) return;

        Material material = player.getInventory().getItemInMainHand().getType();

        Optional<ItemDamageValue> damageValue = itemDamageManager.getObject(material.name());
        damageValue.ifPresentOrElse(itemDamageValue -> event.setDamage(itemDamageValue.getDamage()),
                () -> event.setDamage(event.getDamage() * 0.75));

    }
}
