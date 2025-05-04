package me.mykindos.betterpvp.game.impl.ctf.listener;

import com.google.inject.Inject;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.champions.npc.KitSelectorUseEvent;
import me.mykindos.betterpvp.core.combat.death.events.CustomDeathEvent;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseSkillEvent;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.effects.events.EffectReceiveEvent;
import me.mykindos.betterpvp.game.framework.module.powerup.ParticipantPowerupEvent;
import me.mykindos.betterpvp.game.guice.GameScoped;
import me.mykindos.betterpvp.game.impl.ctf.controller.GameController;
import me.mykindos.betterpvp.game.impl.ctf.model.Flag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Event listener for flag holders
 */
@GameScoped
@CustomLog
public class FlagHolderListener implements Listener {

    private final GameController gameController;
    
    @Inject
    public FlagHolderListener(GameController gameController) {
        this.gameController = gameController;
    }
    
    @EventHandler
    public void onPlayerUseSkill(PlayerUseSkillEvent event) {
        // Prevent flag holders from using skills
        Player player = event.getPlayer();
        if (isHoldingFlag(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSelector(KitSelectorUseEvent event) {
        if (isHoldingFlag(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPowerup(ParticipantPowerupEvent event) {
        if (isHoldingFlag(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onPlayerDeath(CustomDeathEvent event) {
        if (!(event.getKilled() instanceof final Player player)) {
            return;
        }
        dropFlag(player);
    }

    @EventHandler
    public void onGainEffect(EffectReceiveEvent event) {
        if (!(event.getEffect().getEffectType() == EffectTypes.SPEED)) return;
        if (!(event.getTarget() instanceof Player player)) return;

        if (isHoldingFlag(player)) {
            event.cancel("Holding Flag");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        dropFlag(player);
    }

    /**
     * Drops the {@link Flag} if the {@link Player} was holding one and
     * reduces the {@link Flag#getReturnCountdown() return cooldown} by one
     * @param player the {@link Player} to drop the {@link Flag} from
     */
    private void dropFlag(Player player) {
        for (Flag flag : gameController.getFlags().values()) {
            if (player.equals(flag.getHolder())) {
                flag.drop(player.getLocation());
                flag.setReturnCountdown(flag.getReturnCountdown() - 1);
                break;
            }
        }
    }
    
    private boolean isHoldingFlag(Player player) {
        return gameController.getFlags().values().stream()
                .anyMatch(flag -> player.equals(flag.getHolder()));
    }
}