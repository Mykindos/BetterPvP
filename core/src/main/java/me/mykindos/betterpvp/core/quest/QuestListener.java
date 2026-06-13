package me.mykindos.betterpvp.core.quest;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.KillContributionEvent;
import me.mykindos.betterpvp.core.components.professions.PlayerProgressionExperienceEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.quest.model.PrimitiveData;
import me.mykindos.betterpvp.core.world.zone.PlayerEnterZoneEvent;
import me.mykindos.betterpvp.core.world.zone.PlayerExitZoneEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * Bridges core gameplay events to quest objective progress. One listener handles
 * every core trigger type (no class-per-trigger). Leaf modules forward their own
 * events (fishing catches, resource harvests) by calling
 * {@link QuestManager#recordEvent} the same way.
 */
@Singleton
@BPvPListener
public class QuestListener implements Listener {

    private final QuestManager questManager;
    private final AtomicBoolean loaded = new AtomicBoolean(false);

    @Inject
    public QuestListener(QuestManager questManager) {
        this.questManager = questManager;
    }

    /** Warm active instances once, after the quest registry has loaded. */
    @UpdateEvent(delay = 12000)
    public void loadOnce() {
        if (loaded.compareAndSet(false, true)) {
            questManager.loadAll();
        }
    }

    @EventHandler
    public void onKill(KillContributionEvent event) {
        Player killer = event.getKiller();
        if (killer == null || event.getVictim() == null) return;
        final String victimType = event.getVictim().getType().name();
        questManager.recordEvent(killer, "trigger.kill", matchValue("entityType", victimType), 1);
    }

    @EventHandler
    public void onZoneEnter(PlayerEnterZoneEvent event) {
        final String zoneKey = event.getZone().getKey().asString();
        Predicate<PrimitiveData> matcher = matchValue("zone", zoneKey);
        questManager.recordEvent(event.getPlayer(), "trigger.zone_enter", matcher, 1);
        questManager.recordEvent(event.getPlayer(), "trigger.reach_location", matcher, 1);
    }

    @EventHandler
    public void onZoneExit(PlayerExitZoneEvent event) {
        questManager.recordEvent(event.getPlayer(), "trigger.zone_exit", matchValue("zone", event.getZone().getKey().asString()), 1);
    }

    @EventHandler
    public void onProfessionXp(PlayerProgressionExperienceEvent event) {
        int amount = Math.max(1, (int) event.getGainedExp());
        questManager.recordEvent(event.getPlayer(), "trigger.profession_xp", matchValue("profession", event.getProfession()), amount);
    }

    /** Objective matches if its param is unset/blank or equals the event value. */
    private Predicate<PrimitiveData> matchValue(String paramKey, String eventValue) {
        return data -> {
            String configured = data.getString(paramKey);
            return configured == null || configured.isBlank() || configured.equalsIgnoreCase(eventValue);
        };
    }
}
