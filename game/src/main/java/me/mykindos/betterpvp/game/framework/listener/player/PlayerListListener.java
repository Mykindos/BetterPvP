package me.mykindos.betterpvp.game.framework.listener.player;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.combat.death.events.CustomDeathMessageEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.event.GameStateChangeEvent;
import me.mykindos.betterpvp.game.framework.manager.PlayerListManager;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.model.player.PlayerStatsForGame;
import me.mykindos.betterpvp.game.framework.state.GameState;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@BPvPListener
@Singleton
public class PlayerListListener implements Listener {
    private final ServerController serverController;
    private final PlayerController playerController;
    private final PlayerListManager playerListManager;
    private final DamageLogManager damageLogManager;

    @Inject
    public PlayerListListener(PlayerListManager playerListManager, DamageLogManager damageLogManager,
                              PlayerController playerController, ServerController serverController) {
        this.playerListManager = playerListManager;
        this.damageLogManager = damageLogManager;
        this.playerController = playerController;
        this.serverController = serverController;
    }

    @EventHandler
    public void onGameStateChange(GameStateChangeEvent event) {
        if (event.getNewState().equals(GameState.WAITING)) {
            playerListManager.getPlayerStats().clear();
        }

        else if (event.getNewState().equals(GameState.IN_GAME)) {
            for (final @NotNull Player player : playerController.getParticipants().keySet()) {
                playerListManager.getPlayerStats().put(player, new PlayerStatsForGame());
                playerListManager.updatePlayerList(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(CustomDeathMessageEvent event) {
        if (!serverController.getCurrentState().equals(GameState.IN_GAME)) return;
        if (!(event.getKilled() instanceof Player killedPlayer)) return;
        if (!event.getReceiver().equals(killedPlayer)) return;  // ensure only processed once per death
        playerListManager.addDeath(killedPlayer);

        final @Nullable  LivingEntity killer = event.getKiller();
        if (killer == null) return;
        if (!(killer instanceof Player killerPlayer)) return;

        playerListManager.addKill(killerPlayer);
    }
}
