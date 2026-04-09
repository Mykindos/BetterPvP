package me.mykindos.betterpvp.hub.feature;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.CombatFeaturesService;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayObject;
import me.mykindos.betterpvp.core.utilities.model.display.bossbar.BossBarColor;
import me.mykindos.betterpvp.core.utilities.model.display.bossbar.BossBarData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class CombatTimerBossBarListener implements Listener {

    private static final int COMBAT_TIMER_PRIORITY = 10_000;

    private final CombatFeaturesService combatFeaturesService;

    @Inject
    public CombatTimerBossBarListener(CombatFeaturesService combatFeaturesService) {
        this.combatFeaturesService = combatFeaturesService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(ClientJoinEvent event) {
        final Gamer gamer = event.getClient().getGamer();
        gamer.getBossBarQueue().add(COMBAT_TIMER_PRIORITY, BossBarColor.TRANSPARENT, new DisplayObject<>(this::createBossBar));
    }

    private BossBarData createBossBar(Gamer gamer) {
        final Player player = gamer.getPlayer();
        if (player == null || !combatFeaturesService.isActive(player) || !gamer.isInCombat()) {
            return null;
        }

        final long remainingMillis = gamer.getRemainingCombatMillis();
        final int seconds = (int) Math.ceil(remainingMillis / 1000.0D);
        final float progress = (float) remainingMillis / DamageLog.EXPIRY;
        return new BossBarData(UtilMessage.deserialize("<red>In Combat: <dark_red>%s", seconds), progress);
    }
}
