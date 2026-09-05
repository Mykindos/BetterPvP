package me.mykindos.betterpvp.clans.clans.zone;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Keeps every player in the game mode the ground under them asks for.
 * <p>
 * The whole decision lives in the zones: this asks the {@link ZoneManager} what the highest-priority zone with an
 * opinion wants for that player and applies it. Adventure is the answer for anywhere that claims nothing, which makes
 * survival something an area has to opt into - a clan's own land, the wilderness, a training mine - rather than
 * something a world gets by being called {@code world}.
 * <p>
 * Left alone: creative and spectator (staff and menus), frozen players (mid-cutscene), and anyone administrating,
 * since an admin has deliberately chosen the mode they are in.
 */
@BPvPListener
@Singleton
public class ZoneGameModeListener implements Listener {

    private final ZoneManager zoneManager;
    private final ClientManager clientManager;
    private final EffectManager effectManager;

    @Inject
    public ZoneGameModeListener(@NotNull ZoneManager zoneManager, @NotNull ClientManager clientManager,
                                @NotNull EffectManager effectManager) {
        this.zoneManager = zoneManager;
        this.clientManager = clientManager;
        this.effectManager = effectManager;
    }

    @UpdateEvent(delay = 250)
    public void enforceZoneGameMode() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.CREATIVE
                    || player.getGameMode() == GameMode.SPECTATOR
                    || this.effectManager.hasEffect(player, EffectTypes.FROZEN)) {
                continue;
            }

            if (this.clientManager.search().online(player).isAdministrating()) {
                continue;
            }

            final GameMode zoneGameMode = this.zoneManager.getGameModeAt(player);
            final GameMode gameMode = zoneGameMode == null ? GameMode.ADVENTURE : zoneGameMode;
            if (player.getGameMode() != gameMode) {
                player.setGameMode(gameMode);
            }
        }
    }
}
