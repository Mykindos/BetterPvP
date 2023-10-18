package me.mykindos.betterpvp.clans.combat;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.Optional;
import java.util.Random;

@BPvPListener
public class CombatTagListener implements Listener {

    private final GamerManager gamerManager;
    private final EffectManager effectManager;

    private final ClanManager clanManager;

    @Inject
    public CombatTagListener(GamerManager gamerManager, EffectManager effectManager, ClanManager clanManager) {
        this.gamerManager = gamerManager;
        this.effectManager = effectManager;
        this.clanManager = clanManager;
    }

    @UpdateEvent(delay = 1000)
    public void showTag() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId());
            gamerOptional.ifPresent(gamer -> {
                if (!UtilTime.elapsed(gamer.getLastDamaged(), 15000)) {
                    if (effectManager.hasEffect(player, EffectType.INVISIBILITY)) return;

                    if (!clanManager.isInSafeZone(player)) return; // dont do this if player isnt in a safe zone

                    Random rand = new Random();

                    for (int i = 0; i < 10; i++) {
                        double offsetX = (rand.nextDouble() * 2 - 1) * 0.1;
                        double offsetY = (rand.nextDouble() * 2 - 1) * 0.1;
                        double offsetZ = (rand.nextDouble() * 2 - 1) * 0.1;

                        Particle.CRIT.builder()
                                .location(player.getLocation().add(0, 2.5, 0).add(offsetX, offsetY, offsetZ))
                                .receivers(30)
                                .spawn();
                    }
                }
            });
        }
    }
}
