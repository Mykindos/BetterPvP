package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.core.ClanCore;
import me.mykindos.betterpvp.clans.clans.menus.CoreMenu;
import me.mykindos.betterpvp.clans.utilities.ClansNamespacedKeys;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.data.CustomDataType;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Singleton
@BPvPListener
public class ClanCoreCrystalListener implements Listener {

    private final Multimap<ClanCore, Player> nearbyPlayers = HashMultimap.create();
    private final ClanManager clanManager;

    @Inject
    @Config(path = "clans.core.crystal-radius", defaultValue = "5.0")
    private double crystalRadius;

    @Inject
    public ClanCoreCrystalListener(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand().equals(EquipmentSlot.OFF_HAND)) {
            return;
        }

        final PersistentDataContainer pdc = event.getRightClicked().getPersistentDataContainer();
        if (!pdc.has(ClansNamespacedKeys.CLAN_CORE)) {
            return;
        }

        event.setCancelled(true);
        final UUID clanId = Objects.requireNonNull(pdc.get(ClansNamespacedKeys.CLAN, CustomDataType.UUID));
        final Clan clan = clanManager.getClanById(clanId).orElseThrow();
        if (clanManager.getClanByPlayer(event.getPlayer()).orElse(null) != clan) {
            UtilMessage.message(event.getPlayer(), "Clans", "You cannot use this clan core.");
            return;
        }

        new CoreMenu(clan, event.getPlayer()).show(event.getPlayer());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!event.getEntity().getPersistentDataContainer().has(ClansNamespacedKeys.CLAN_CORE)) {
            return;
        }

        event.setCancelled(true);
        // todo: handle damage
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!event.getEntity().getPersistentDataContainer().has(ClansNamespacedKeys.CLAN_CORE)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent event) {
        if (!event.hasChangedPosition()) {
            return; // Player has not moved
        }

        final Optional<Clan> clanOpt = this.clanManager.getClanByChunk(event.getTo().getChunk());
        if (clanOpt.isEmpty()) {
            return; // Player is not in a clan chunk
        }

        final Clan clan = clanOpt.get();
        ClanCore core = clan.getCore();
        final Player player = event.getPlayer();
        if (!core.isSet() || core.hasJustTeleported() || clan.getMemberByUUID(player.getUniqueId()).isEmpty()) {
            return; // Player is not in the clan or the core is not set
        }

        if (Objects.requireNonNull(core.getPosition()).distanceSquared(event.getTo()) > Math.pow(crystalRadius, 2)) {
            return; // Player is not near the core
        }

        core.setVisible(true);

        if (!nearbyPlayers.containsEntry(core, player)) {
            nearbyPlayers.put(core, player);
            core.show(player);
        }
    }

    @UpdateEvent
    public void onTick() {
        // Get relative particle loc to avoid duplicate calculations
        // Do a rotating circle around the core
        final long time = System.currentTimeMillis() / 7;
        final double angle = Math.toRadians(time % 360);
        final double amplitude = 2.0 + 0.5 * Math.sin(Math.toRadians(time % 360)); // Oscillates between 1.2 and 2.0
        final double x = Math.cos(angle) * amplitude;
        final double y = Math.sin(angle) * amplitude / 3;
        final double negY = Math.cos(angle) * amplitude / 3;
        final double z = Math.sin(angle) * amplitude;

        for (ClanCore core : new HashSet<>(nearbyPlayers.keySet())) { // Copy to avoid concurrent modification
            if (!core.isSet()) {
                nearbyPlayers.removeAll(core);
                core.setVisible(false);
                continue;
            }

            // Remove player if they are dead or too far away
            final Location corePos = Objects.requireNonNull(core.getPosition());
            for (Player player : new ArrayList<>(nearbyPlayers.get(core))) { // Copy to avoid concurrent modification
                if (!player.isValid()
                        || player.getGameMode().equals(GameMode.SPECTATOR)
                        || player.getLocation().distanceSquared(corePos) > Math.pow(crystalRadius, 2)) {
                    nearbyPlayers.remove(core, player);
                    core.hide(player);
                }
            }

            // Set invisible if no players are nearby
            if (core.isVisible() && nearbyPlayers.get(core).isEmpty() && !clanManager.getPillageHandler().isBeingPillaged(core.getClan())) {
                core.setVisible(false);
                nearbyPlayers.removeAll(core);
                continue;
            }

            // Tick particles
            final EnderCrystal crystal = core.getCrystal();
            if (crystal != null && crystal.isValid()) {
                final Location point = crystal.getLocation().add(0.0, crystal.getHeight() / 2.0, 0.0);
                Particle.SPELL_WITCH.builder()
                        .location(point.clone().add(x, y , z))
                        .extra(0)
                        .receivers(nearbyPlayers.get(core))
                        .spawn();

                Particle.SPELL_WITCH.builder()
                        .location(point.clone().add(-x, negY, -z))
                        .extra(0)
                        .receivers(nearbyPlayers.get(core))
                        .spawn();
            }
        }
    }

}
