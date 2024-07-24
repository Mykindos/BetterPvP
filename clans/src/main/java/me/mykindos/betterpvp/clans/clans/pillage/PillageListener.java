package me.mykindos.betterpvp.clans.clans.pillage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanProperty;
import me.mykindos.betterpvp.clans.clans.insurance.Insurance;
import me.mykindos.betterpvp.clans.clans.pillage.events.PillageEndEvent;
import me.mykindos.betterpvp.clans.clans.pillage.events.PillageStartEvent;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@BPvPListener
@Singleton
@CustomLog
public class PillageListener implements Listener {

    private static final DecimalFormat df = new DecimalFormat("0.0");

    @Inject
    @Config(path = "clans.pillage.duration", defaultValue = "10")
    private int pillageDurationInMinutes;

    @Inject
    @Config(path = "clans.pillage.noDominanceCooldown", defaultValue = "4")
    private int noDominanceCooldownHours;

    @Inject
    @Config(path = "clans.pillage.timeRemovedOnKill", defaultValue = "0")
    private int timeRemoveOnKill;


    private final Clans clans;
    private final PillageHandler pillageHandler;
    private final ClanManager clanManager;
    private final DamageLogManager damageLogManager;

    @Inject
    public PillageListener(Clans clans, ClanManager clanManager, PillageHandler pillageHandler, DamageLogManager damageLogManager) {
        this.clans = clans;
        this.clanManager = clanManager;
        this.pillageHandler = pillageHandler;
        this.damageLogManager = damageLogManager;
    }

    @UpdateEvent(delay = 5000)
    public void checkActivePillages() {
        if (pillageHandler.getActivePillages().isEmpty()) return;

        pillageHandler.getActivePillages().forEach(pillage -> {
            if (pillage.getPillageFinishTime() - System.currentTimeMillis() <= 0) {
                UtilServer.runTaskLater(clans, () -> UtilServer.callEvent(new PillageEndEvent(pillage)), 1);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPillageStart(PillageStartEvent event) {
        Pillage pillage = event.getPillage();
        pillage.setPillageStartTime(System.currentTimeMillis());
        pillage.setPillageFinishTime(System.currentTimeMillis() + (pillageDurationInMinutes * 60000L));
        pillage.setAbsolutionFinishTime(pillage.getPillageFinishTime());

        pillageHandler.startPillage(event.getPillage());

        Clan pillaged = (Clan) pillage.getPillaged();
        Clan pillager = (Clan) pillage.getPillager();

        ClanEnemy pillagerEnemy = pillager.getEnemy(pillaged).orElseThrow();
        ClanEnemy pillagedEnemy = pillaged.getEnemy(pillager).orElseThrow();

        pillager.getEnemies().remove(pillagerEnemy);
        pillaged.getEnemies().remove(pillagedEnemy);
        clanManager.getRepository().deleteClanEnemy(pillager, pillagerEnemy);
        clanManager.getRepository().deleteClanEnemy(pillaged, pillagedEnemy);


        pillaged.putProperty(ClanProperty.NO_DOMINANCE_COOLDOWN, (System.currentTimeMillis() + (3_600_000L * noDominanceCooldownHours)));

        if (pillaged.getTntRecoveryRunnable() != null) {

            pillaged.getTntRecoveryRunnable().cancel();
            pillaged.setTntRecoveryRunnable(null);
        }


        log.info("{} ({}) started a pillage against {} ({})", pillager.getName(), pillager.getId(), pillaged.getName(), pillaged.getId())
                .addClanContext(pillager).addClanContext(pillaged, true).submit();
        UtilMessage.broadcast(UtilMessage.deserialize("<blue>Clans> <red>%s <gray>has started a pillage on <red>%s<gray>!", pillager.getName(), pillaged.getName()));
        Bukkit.getOnlinePlayers().forEach(player -> UtilSound.playSound(player, Sound.ITEM_GOAT_HORN_SOUND_1, 1f, 0.8f, true));

        // Grant points
        int pointsGained = Math.max(1, pillaged.getSquadCount() - pillager.getSquadCount());
        pillager.saveProperty(ClanProperty.POINTS.name(), pillager.getPoints() + pointsGained);
        pillager.messageClan("Your clan has earned <green>" + pointsGained + "<reset> points.", null, true);

        // Deduct points
        pillaged.saveProperty(ClanProperty.POINTS.name(), Math.max(0, pillaged.getPoints() - pointsGained));
        pillaged.messageClan("Your clan has lost <red>" + pointsGained + "<reset> points.", null, true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPillageEnd(PillageEndEvent event) {
        pillageHandler.endPillage(event.getPillage());
        Clan clan = (Clan) event.getPillage().getPillaged();

        clan.messageClan("The raid on your clan has ended, and your base will begin to regenerate in <green>2</green> minutes.", null, true);

        UtilServer.runTaskLater(clans, () -> {
            List<Insurance> insuranceList = clan.getInsurance();
            insuranceList.sort(Collections.reverseOrder());
            clanManager.getInsuranceQueue().addAll(insuranceList);
            clanManager.getRepository().deleteInsuranceForClan(clan);
            clan.getInsurance().clear();
        }, 20 * 60 * 2L);
    }

    @UpdateEvent(delay = 30000)
    public void notifyPillageDuration() {
        if (pillageHandler.getActivePillages().isEmpty()) return;

        pillageHandler.getActivePillages().forEach(this::notifyPillageTime);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player killed = event.getPlayer();
        DamageLog lastDamaged = damageLogManager.getLastDamager(killed);
        if (lastDamaged == null) return;
        if (!(lastDamaged.getDamager() instanceof Player killer)) return;

        Optional<Clan> killedClanOptional = clanManager.getClanByPlayer(killed);
        if (killedClanOptional.isEmpty()) {
            return;
        }

        Optional<Clan> killerClanOptional = clanManager.getClanByPlayer(killer);
        if (killerClanOptional.isEmpty()) {
            return;
        }

        Clan killerClan = killerClanOptional.get();
        Clan killedClan = killedClanOptional.get();

        Pillage pillage = pillageHandler.getActivePillages().stream()
                .filter(p -> p.getPillaged().equals(killerClan) && p.getPillager().equals(killedClan))
                .findFirst().orElse(null);

        if (pillage != null) {
            pillage.setPillageFinishTime(pillage.getPillageFinishTime() - (timeRemoveOnKill * 1000L));

            killerClan.messageClan("As your clan killed an attacker, the remaining pillage time has been reduced by <green>"
                    + timeRemoveOnKill + " <gray>seconds.", null, true);
            killedClan.messageClan("As your clan was killed by a defender, the remaining pillage time has been reduced by <green>"
                    + timeRemoveOnKill + " <gray>seconds.", null, true);
            notifyPillageTime(pillage);
            checkActivePillages();
        }

    }

    private void notifyPillageTime(Pillage pillage) {
        if (pillage.getPillageFinishTime() > System.currentTimeMillis()) {
            String minutesRemaining = df.format((double) (pillage.getPillageFinishTime() - System.currentTimeMillis()) / 60000);
            pillage.getPillaged().messageClan("<gray>The pillage on your clan ends in <green>"
                    + minutesRemaining + " <gray>minutes.", null, true);
            pillage.getPillager().messageClan("<gray>The pillage on <red>" + pillage.getPillaged().getName()
                    + "<gray> ends in <green>" + minutesRemaining + " <gray>minutes.", null, true);
        }
    }

    @EventHandler
    public void onDropLoot(PlayerDropItemEvent event) {
        if (event.getPlayer().getOpenInventory().getType() == InventoryType.CRAFTING) return;

        Optional<Clan> plyerClanOptional = clanManager.getClanByPlayer(event.getPlayer());
        if (plyerClanOptional.isEmpty()) return;

        Optional<Clan> locationClanOptional = clanManager.getClanByLocation(event.getPlayer().getLocation());
        if (locationClanOptional.isEmpty()) return;

        Clan playerClan = plyerClanOptional.get();
        Clan locationClan = locationClanOptional.get();

        if (pillageHandler.isPillaging(playerClan, locationClan)) {
            UtilMessage.simpleMessage(event.getPlayer(), "Clans", "You cannot drop items directly from storage during a pillage!");
            event.setCancelled(true);
        }
    }
}
