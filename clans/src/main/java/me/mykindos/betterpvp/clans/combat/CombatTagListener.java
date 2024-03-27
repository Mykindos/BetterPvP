package me.mykindos.betterpvp.clans.combat;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.time.Duration;
import java.util.Random;

@BPvPListener
public class CombatTagListener implements Listener {
    private final ClientManager clientManager;
    private final EffectManager effectManager;
    private final ClanManager clanManager;

    @Inject
    public CombatTagListener(ClientManager clientManager, EffectManager effectManager, ClanManager clanManager) {
        this.clientManager = clientManager;
        this.effectManager = effectManager;
        this.clanManager = clanManager;
    }

    @UpdateEvent(delay = 1000)
    public void showTag() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            final Gamer gamer = clientManager.search().online(player).getGamer();
            if (!UtilTime.elapsed(gamer.getLastDamaged(), 15000)) {
                if (effectManager.hasEffect(player, EffectTypes.VANISH)) return;

                if (!clanManager.isInSafeZone(player)) return; // don't do this if player isn't in a safe zone

                Random rand = UtilMath.RANDOM;

                for (int i = 0; i < 10; i++) {
                    double offsetX = (rand.nextDouble() * 2 - 1) * 0.25;
                    double offsetY = (rand.nextDouble() * 2 - 1) * 0.25;
                    double offsetZ = (rand.nextDouble() * 2 - 1) * 0.25;

                    Particle.CRIT.builder()
                            .location(player.getLocation().add(0, 2.25, 0).add(offsetX, offsetY, offsetZ))
                            .extra(0).receivers(30).spawn();
                }
            }
        }
    }

    @UpdateEvent
    public void showSafetySubtitle() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            Gamer gamer = clientManager.search().online(player).getGamer();

            if (!UtilTime.elapsed(gamer.getLastDamaged(), 15000)) {
                if (clanManager.isInSafeZone(player)) {

                    long remainingMillis = 15000 - (System.currentTimeMillis() - gamer.getLastDamaged());
                    double remainingSeconds = remainingMillis / 1000.0;

                    Component subtitleText = UtilMessage.deserialize("<gray>Unsafe for: <red>" + String.format("%.1f", remainingSeconds) + "s");
                    player.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(400), Duration.ofMillis(0)));
                    player.sendTitlePart(TitlePart.TITLE, Component.text(""));
                    player.sendTitlePart(TitlePart.SUBTITLE, subtitleText);
                }
            }
        }
    }

}
