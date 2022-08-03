package me.mykindos.betterpvp.clans.scoreboards;

import me.mykindos.betterpvp.core.framework.events.scoreboard.ScoreboardUpdateEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

@BPvPListener
public class ClansHealthScoreboardListener implements Listener {

    @EventHandler
    public void updateHealthBarScoreboard(ScoreboardUpdateEvent event) {
        Player player = event.getPlayer();
        Scoreboard scoreboard = player.getScoreboard();

        Objective healthObjective = scoreboard.getObjective("healthDisplay");
        if (healthObjective == null) {
            healthObjective = scoreboard.registerNewObjective("healthDisplay", "dummy", Component.text(ChatColor.RED + "\u2764"));
            healthObjective.setDisplaySlot(DisplaySlot.BELOW_NAME);
        }
    }

    @UpdateEvent(delay=200)
    public void updateHealth(){
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            Objective health = onlinePlayer.getScoreboard().getObjective("healthDisplay");
            if (health != null) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    health.getScore(player.getName()).setScore((int) player.getHealth());
                }
            }
        }
    }

}
