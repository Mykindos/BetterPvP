package me.mykindos.betterpvp.clans.combat;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.gamer.Gamer;
import me.mykindos.betterpvp.clans.gamer.GamerManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.Optional;

@BPvPListener
public class CombatListener implements Listener {

    private final GamerManager gamerManager;
    private final EffectManager effectManager;

    @Inject
    public CombatListener(GamerManager gamerManager, EffectManager effectManager) {
        this.gamerManager = gamerManager;
        this.effectManager = effectManager;
    }

    @UpdateEvent(delay = 1000)
    public void showTag() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId());
            gamerOptional.ifPresent(gamer -> {
                if (!UtilTime.elapsed(gamer.getLastDamaged(), 15000)) {
                    if (effectManager.hasEffect(player, EffectType.INVISIBILITY)) return;

                    Particle.VILLAGER_HAPPY.builder().allPlayers().location(player.getLocation().add(0, 4, 0)).spawn();
                }
            });

        }

    }
}
