package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.core.ClanCore;
import me.mykindos.betterpvp.clans.clans.menus.CoreMenu;
import me.mykindos.betterpvp.clans.clans.pillage.Pillage;
import me.mykindos.betterpvp.clans.clans.pillage.events.PillageEndEvent;
import me.mykindos.betterpvp.clans.clans.pillage.events.PillageStartEvent;
import me.mykindos.betterpvp.clans.utilities.ClansNamespacedKeys;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.data.CustomDataType;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    @Config(path = "clans.core.crystal-enabled", defaultValue = "true")
    private boolean enabled;

    @Inject
    @Config(path = "clans.core.crystal-helix-enabled", defaultValue = "true")
    private boolean helixEnabled;

    @Inject
    @Config(path = "clans.core.crystal-health", defaultValue = "200.0")
    private double crystalHealth;

    @Inject
    public ClanCoreCrystalListener(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(DamageEvent event) {
        if (!enabled) {
            return;
        }

        if (!event.getDamagee().getPersistentDataContainer().has(ClansNamespacedKeys.CLAN_CORE)) {
            return;
        }

        final Clan clan = clanManager.getClanByChunk(event.getDamagee().getChunk()).orElseThrow();
        final ClanCore core = clan.getCore();
        final LivingEntity damagerEnt = event.getDamager();
        if (core.isDead() || !(damagerEnt instanceof Player damager)) {
            return;
        }

        final Clan other = clanManager.getClanByPlayer(damager).orElse(null);
        if (!this.clanManager.getPillageHandler().isPillaging(other, clan)) {
            return;
        }

        core.setHealth(core.getHealth() - event.getDamage());
        event.setDamage(-1); // Cancel damage application so we still get damage delay
        new SoundEffect(Sound.BLOCK_ANVIL_PLACE, 2f, 0.2f).play(event.getDamagee().getLocation());
        new SoundEffect(Sound.ENTITY_ALLAY_HURT, 1.6f, 0.4f).play(event.getDamagee().getLocation());
        new SoundEffect(Sound.ENTITY_ALLAY_HURT, 0.4f, 0.4f).play(event.getDamagee().getLocation());

        if (core.isDead()) {
            final SoundEffect sound = new SoundEffect(Sound.ENTITY_WITHER_DEATH, 2f, 0.8f);
            final List<String> clanNames = new ArrayList<>();
            for (Pillage pillage : this.clanManager.getPillageHandler().getPillagesOn(clan)) {
                pillage.getPillager().messageClan("<red>" + clan.getName() + "</red>'s core has been destroyed. <green><b>Full block access enabled.", null, true);
                clanNames.add(pillage.getPillager().getName());
                for (Player player : pillage.getPillager().getMembersAsPlayers()) {
                    sound.play(player);
                }
            }

            final String names = String.join(", ", clanNames);
            clan.messageClan("<red>Your core has been destroyed. <green><b>Full block access enabled to " + names + ".", null, true);
            for (Player player : clan.getMembersAsPlayers()) {
                sound.play(player);
            }
            setInvisible(core);
            return;
        }

        updateHealthbar(core);
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!enabled || event.getHand().equals(EquipmentSlot.OFF_HAND)) {
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
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!enabled || !event.getEntity().getPersistentDataContainer().has(ClansNamespacedKeys.CLAN_CORE)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onPillageStart(PillageStartEvent event) {
        final Clan pillaged = (Clan) event.getPillage().getPillaged();
        final ClanCore core = pillaged.getCore();
        if (core.isSet() && this.clanManager.getPillageHandler().getPillagesOn(pillaged).isEmpty()) {
            core.setHealth(crystalHealth); // set health back to full when last pillage ends
            core.setVisible(true);
            final EnderCrystal crystal = Objects.requireNonNull(core.getCrystal());
            crystal.setVisibleByDefault(true);

            // Healthbar
            crystal.getWorld().spawn(crystal.getLocation().add(0, crystal.getHeight(), 0), TextDisplay.class, entity -> {
                entity.setInvulnerable(true);
                entity.setGravity(false);
                entity.setVisualFire(false);
                entity.setPersistent(false);
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                entity.setSeeThrough(false);
                entity.setShadowed(true);
                crystal.addPassenger(entity);
            });
            updateHealthbar(core);
        }
    }

    @EventHandler
    public void onPillageEnd(PillageEndEvent event) {
        final Clan pillaged = (Clan) event.getPillage().getPillaged();
        final ClanCore core = pillaged.getCore();
        if (core.isSet() && this.clanManager.getPillageHandler().getPillagesOn(pillaged).size() == 1) {
             core.setHealth(crystalHealth); // set health back to full when last pillage ends
             setInvisible(core);
        }
    }

    private void setInvisible(ClanCore core) {
        final EnderCrystal crystal = core.getCrystal();
        if (crystal == null) {
            return;
        }

        crystal.setVisibleByDefault(false);
        for (Entity passenger : crystal.getPassengers()) {
            if (passenger instanceof TextDisplay && passenger.isValid()) {
                passenger.remove();
            }
        }

        core.setVisible(false);
    }

    private void updateHealthbar(ClanCore core) {
        if (core.getCrystal() == null) {
            return;
        }

        final EnderCrystal crystal = core.getCrystal();
        for (Entity passenger : crystal.getPassengers()) {
            if (passenger instanceof TextDisplay healthBar && passenger.isValid()) {
                final double progress = core.getHealth() / crystalHealth;
                healthBar.text(ProgressBar.withProgress((float) progress).build());
            }
        }
    }

    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent event) {
        if (!enabled || !event.hasChangedBlock()) {
            return; // Player has not moved
        }

        final Optional<Clan> clanOpt = this.clanManager.getClanByChunk(event.getTo().getChunk());
        if (clanOpt.isEmpty()) {
            return; // Player is not in a clan chunk
        }

        final Clan clan = clanOpt.get();
        final ClanCore core = clan.getCore();
        final Player player = event.getPlayer();
        if (this.clanManager.getPillageHandler().isBeingPillaged(clan)
                || !core.isSet() || core.hasJustTeleported()
                || this.clanManager.getClanByPlayer(player).orElse(null) != clan) {
            return; // Player is not in the clan or the core is not set
        }

        if (Objects.requireNonNull(core.getPosition()).distanceSquared(event.getTo()) > Math.pow(crystalRadius, 2)) {
            return; // Player is not near the core
        }

        if (!UtilLocation.isInFront(player, core.getPosition().clone().add(0.0, 1.0, 0))) {
            return;
        }

        core.setVisible(true);

        if (!nearbyPlayers.containsEntry(core, player)) {
            nearbyPlayers.put(core, player);
            core.show(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!enabled) {
            return;
        }

        nearbyPlayers.values().remove(event.getPlayer());
    }

    @UpdateEvent
    public void onTick() {
        if (!enabled) {
            return;
        }

        // Get relative particle loc to avoid duplicate calculations
        // Do a rotating circle around the core
        double x = 0;
        double y = 0;
        double negY = 0;
        double z = 0;
        if (helixEnabled) {
            final long time = System.currentTimeMillis() / 7;
            final double angle = Math.toRadians(time % 360);
            final double cos = Math.cos(angle);
            final double sin = Math.sin(angle);
            final double amplitude = 2.0 + 0.5 * sin; // Oscillates between 1.5 and 2.5
            x = cos * amplitude;
            y = sin * amplitude / 3;
            negY = cos * amplitude / 3;
            z = sin * amplitude;
        }

        for (ClanCore core : new HashSet<>(nearbyPlayers.keySet())) { // Copy to avoid concurrent modification
            if (this.clanManager.getPillageHandler().isBeingPillaged(core.getClan())) {
                continue;
            }

            if (!core.isSet()) {
                nearbyPlayers.removeAll(core);
                core.setVisible(false);
                continue;
            }

            for (Player player : new ArrayList<>(nearbyPlayers.get(core))) {
                if (!player.isValid() || player.getGameMode() == GameMode.SPECTATOR
                        || player.getLocation().distanceSquared(Objects.requireNonNull(core.getPosition())) > Math.pow(crystalRadius, 2)
                        || !UtilLocation.isInFront(player, core.getPosition().clone().add(0, 0.5, 0))) {
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
                crystal.setFireTicks(0);

                if (helixEnabled) {
                    final Location point = crystal.getLocation().add(0.0, crystal.getHeight() / 2.0, 0.0);
                    Particle.SPELL_WITCH.builder()
                            .location(point.clone().add(x, y, z))
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

}
