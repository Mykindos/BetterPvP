package me.mykindos.betterpvp.clans.clans.pillage;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanProperty;
import me.mykindos.betterpvp.clans.clans.insurance.Insurance;
import me.mykindos.betterpvp.clans.clans.pillage.events.PillageEndEvent;
import me.mykindos.betterpvp.clans.clans.pillage.events.PillageStartEvent;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;

@BPvPListener
public class PillageListener implements Listener {

    private static final DecimalFormat df = new DecimalFormat("0.0");

    @Inject
    @Config(path = "clans.pillage.duration", defaultValue = "10")
    private int pillageDurationInMinutes;

    @Inject
    @Config(path = "clans.pillage.noDominanceCooldown", defaultValue = "4")
    private int noDominanceCooldownHours;

    private final Clans clans;
    private final PillageHandler pillageHandler;
    private final ClanManager clanManager;

    @Inject
    public PillageListener(Clans clans, ClanManager clanManager, PillageHandler pillageHandler) {
        this.clans = clans;
        this.clanManager = clanManager;
        this.pillageHandler = pillageHandler;
    }

    @UpdateEvent(delay = 5000)
    public void checkActivePillages() {
        if (pillageHandler.getActivePillages().isEmpty()) return;

        pillageHandler.getActivePillages().forEach(pillage -> {
            if (pillage.getPillageFinishTime() - System.currentTimeMillis() <= 0) {
                UtilServer.runTaskLater(clans, () ->UtilServer.callEvent(new PillageEndEvent(pillage)), 1);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPillageStart(PillageStartEvent event) {
        Pillage pillage = event.getPillage();
        pillage.setPillageStartTime(System.currentTimeMillis());
        pillage.setPillageFinishTime(System.currentTimeMillis() + (pillageDurationInMinutes * 60000L));

        pillageHandler.startPillage(event.getPillage());

        ClanEnemy pillagerEnemy = pillage.getPillager().getEnemy(pillage.getPillaged()).orElseThrow();
        ClanEnemy pillagedEnemy = pillage.getPillaged().getEnemy(pillage.getPillager()).orElseThrow();

        pillage.getPillager().getEnemies().remove(pillagerEnemy);
        pillage.getPillaged().getEnemies().remove(pillagedEnemy);
        clanManager.getRepository().deleteClanEnemy(pillage.getPillager(), pillagerEnemy);
        clanManager.getRepository().deleteClanEnemy(pillage.getPillaged(), pillagedEnemy);

        if(pillage.getPillaged() instanceof Clan pillagedClan) {
            pillagedClan.putProperty(ClanProperty.NO_DOMINANCE_COOLDOWN, (System.currentTimeMillis() + (3_600_000L * noDominanceCooldownHours)));

            if(pillagedClan.getTntRecoveryRunnable() != null) {
                pillagedClan.getTntRecoveryRunnable().cancel();
                pillagedClan.setTntRecoveryRunnable(null);
            }
        }
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

        pillageHandler.getActivePillages().forEach(pillage -> {
            String minutesRemaining = df.format((double) (pillage.getPillageFinishTime() - System.currentTimeMillis()) / 60000);
            pillage.getPillaged().messageClan("<gray>The pillage on your clan ends in <green>"
                    + minutesRemaining + " <gray>minutes.", null, true);
            pillage.getPillager().messageClan("<gray>The pillage on <red>" + pillage.getPillaged().getName()
                    + "<gray> ends in <green>" + minutesRemaining + " <gray>minutes.", null, true);
        });
    }

}
