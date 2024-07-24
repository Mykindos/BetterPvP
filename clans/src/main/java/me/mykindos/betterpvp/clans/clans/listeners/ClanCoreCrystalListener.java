package me.mykindos.betterpvp.clans.clans.listeners;

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
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.data.CustomDataType;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Singleton
@BPvPListener
public class ClanCoreCrystalListener implements Listener {

    private final ClanManager clanManager;

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
    public ClanCoreCrystalListener(final ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(final DamageEvent event) {
        if (!this.enabled) {
            return;
        }

        final PersistentDataContainer pdc = event.getDamagee().getPersistentDataContainer();
        if (!pdc.has(ClansNamespacedKeys.CLAN_CORE)) {
            return;
        }

        final UUID clanId = Objects.requireNonNull(pdc.get(ClansNamespacedKeys.CLAN, CustomDataType.UUID));
        final Clan clan = this.clanManager.getClanById(clanId).orElseThrow();
        final ClanCore core = clan.getCore();
        final LivingEntity damagerEnt = event.getDamager();
        if (core.isDead() || !(damagerEnt instanceof final Player damager)) {
            return;
        }

        final Clan other = this.clanManager.getClanByPlayer(damager).orElse(null);
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
            for (final Pillage pillage : this.clanManager.getPillageHandler().getPillagesOn(clan)) {
                pillage.getPillager().messageClan("<red>" + clan.getName() + "</red>'s core has been destroyed. <green><b>Full block access enabled.", null, true);
                clanNames.add(pillage.getPillager().getName());
                for (final Player player : pillage.getPillager().getMembersAsPlayers()) {
                    sound.play(player);
                }
            }

            final String names = String.join(", ", clanNames);
            clan.messageClan("<red>Your core has been destroyed. <green><b>Full block access enabled for " + names + ".", null, true);
            for (final Player player : clan.getMembersAsPlayers()) {
                sound.play(player);
            }
            core.despawnCrystal();
            return;
        }

        core.updateHealthBar();
    }

    @EventHandler
    public void onInteractEntity(final PlayerInteractEntityEvent event) {
        if (!this.enabled || event.getHand().equals(EquipmentSlot.OFF_HAND)) {
            return;
        }

        final PersistentDataContainer pdc = event.getRightClicked().getPersistentDataContainer();
        if (!pdc.has(ClansNamespacedKeys.CLAN_CORE)) {
            return;
        }

        event.setCancelled(true);
        final UUID clanId = Objects.requireNonNull(pdc.get(ClansNamespacedKeys.CLAN, CustomDataType.UUID));
        final Clan clan = this.clanManager.getClanById(clanId).orElseThrow();
        if (this.clanManager.getClanByPlayer(event.getPlayer()).orElse(null) != clan) {
            UtilMessage.message(event.getPlayer(), "Clans", "You cannot use this clan core.");
            return;
        }

        new CoreMenu(clan, event.getPlayer()).show(event.getPlayer());
    }

    @EventHandler
    public void onEntityExplode(final EntityExplodeEvent event) {
        if (!this.enabled || !event.getEntity().getPersistentDataContainer().has(ClansNamespacedKeys.CLAN_CORE)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onPillageStart(final PillageStartEvent event) {
        if (!this.enabled) {
            return;
        }

        final Clan pillaged = (Clan) event.getPillage().getPillaged();
        final ClanCore core = pillaged.getCore();
        if (core.isSet() && this.clanManager.getPillageHandler().getPillagesOn(pillaged).isEmpty()) {
            core.setMaxHealth(this.crystalHealth);
            core.setHealth(this.crystalHealth);
            core.spawnCrystal(true);
        }
    }

    @EventHandler
    public void onPillageEnd(final PillageEndEvent event) {
        if (!this.enabled) {
            return;
        }

        final Clan pillaged = (Clan) event.getPillage().getPillaged();
        final ClanCore core = pillaged.getCore();
        if (core.isSet() && this.clanManager.getPillageHandler().getPillagesOn(pillaged).size() == 1) {
            core.setHealth(this.crystalHealth); // set health back to full when last pillage ends
            core.despawnCrystal();
        }
    }

}
