package me.mykindos.betterpvp.clans.display;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import org.bukkit.event.Listener;

@PluginAdapter("Clans")
@Singleton
public class HealthIndicator implements Listener {

    protected HealthIndicator() {
//        final Scoreboard healthSb = Bukkit.getScoreboardManager().getNewScoreboard();
//        final Objective objective = healthSb.registerNewObjective("health",
//                Criteria.HEALTH,
//                Component.text("\u2764", NamedTextColor.RED),
//                RenderType.INTEGER);
//
//        objective.setAutoUpdateDisplay(true);
//        objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
    }

}
