package me.mykindos.betterpvp.game.impl.ctf.listener;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.stats.impl.game.CTFGameStat;
import me.mykindos.betterpvp.core.combat.death.events.CustomDeathEvent;
import me.mykindos.betterpvp.game.framework.listener.player.event.ParticipantPreRespawnEvent;
import me.mykindos.betterpvp.game.framework.model.stats.StatManager;
import me.mykindos.betterpvp.game.guice.GameScoped;
import me.mykindos.betterpvp.game.impl.ctf.controller.GameController;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@GameScoped
public class SuddenDeathListener implements Listener {

    private final GameController gameController;
    private final StatManager statManager;

    @Inject
    public SuddenDeathListener(GameController gameController, StatManager statManager) {
        this.gameController = gameController;
        this.statManager = statManager;
    }

    @EventHandler
    public void onPlayerRespawn(ParticipantPreRespawnEvent event) {
        if (gameController.isSuddenDeath()) {
            event.setCancelled(true); // No respawning in sudden death
        }
    }

    @EventHandler
    public void onCustomDeath(CustomDeathEvent event) {
        if (!gameController.isSuddenDeath()) return;
        if (event.getKiller() instanceof Player killer) {
            final CTFGameStat.CTFGameStatBuilder<?, ?> killerBuilder =  CTFGameStat.builder()
                    .action(CTFGameStat.Action.SUDDEN_DEATH_KILLS);
            statManager.incrementStat(killer.getUniqueId(), killerBuilder, 1);
        }

        if (event.getKilled() instanceof Player killed) {
            final CTFGameStat.CTFGameStatBuilder<?, ?> killedBuilder =  CTFGameStat.builder()
                    .action(CTFGameStat.Action.SUDDEN_DEATH_DEATHS);
            statManager.incrementStat(killed.getUniqueId(), killedBuilder, 1);
        }
    }
}
