package me.mykindos.betterpvp.clans.clans.pillage;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.ClanManager;
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

@BPvPListener
public class PillageListener implements Listener {

    private static final DecimalFormat df = new DecimalFormat("0.0");

    @Inject
    @Config(path = "clans.pillage.duration", defaultValue = "10")
    private int pillageDurationInMinutes;

    private final PillageHandler pillageHandler;
    private final ClanManager clanManager;

    @Inject
    public PillageListener(ClanManager clanManager, PillageHandler pillageHandler) {
        this.clanManager = clanManager;
        this.pillageHandler = pillageHandler;
    }

    @UpdateEvent(delay = 5000)
    public void checkActivePillages() {
        if (pillageHandler.getActivePillages().isEmpty()) return;

        pillageHandler.getActivePillages().forEach(pillage -> {
            if (pillage.getPillageFinishTime() - System.currentTimeMillis() <= 0) {
                UtilServer.callEvent(new PillageEndEvent(pillage));
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
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPillageEnd(PillageEndEvent event) {
        pillageHandler.endPillage(event.getPillage());
    }

    @UpdateEvent(delay = 60000)
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
