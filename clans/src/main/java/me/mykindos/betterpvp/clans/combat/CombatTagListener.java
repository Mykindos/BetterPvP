package me.mykindos.betterpvp.clans.combat;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
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
import java.util.*;

@BPvPListener
public class CombatTagListener implements Listener {
    private Set<UUID> playersShownSafeMessage = new HashSet<>();
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

                    if (!clanManager.isInSafeZone(player)) return; // don't do this if player isn't in a safe zone

                    Random rand = new Random();

                    for (int i = 0; i < 10; i++) {
                        double offsetX = (rand.nextDouble() * 2 - 1) * 0.25;
                        double offsetY = (rand.nextDouble() * 2 - 1) * 0.25;
                        double offsetZ = (rand.nextDouble() * 2 - 1) * 0.25;

                        Particle.CRIT.builder()
                                .location(player.getLocation().add(0, 2.25, 0).add(offsetX, offsetY, offsetZ))
                                .extra(0).receivers(30).spawn();
                    }
                }
            });
        }
    }

    @UpdateEvent(delay = 100)
    public void showSafetySubtitle() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId());
            gamerOptional.ifPresent(gamer -> {
                UUID playerId = player.getUniqueId();

                if (!UtilTime.elapsed(gamer.getLastDamaged(), 15000)) {
                    if (clanManager.isInSafeZone(player)) {
                        playersShownSafeMessage.remove(playerId);

                        long remainingMillis = 15000 - (System.currentTimeMillis() - gamer.getLastDamaged());
                        double remainingSeconds = remainingMillis / 1000.0;

                        Component subtitleText = UtilMessage.deserialize("<gray>Unsafe for: <red>" + String.format("%.1f", remainingSeconds) + "s");
                        player.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(250), Duration.ofMillis(0)));
                        player.sendTitlePart(TitlePart.SUBTITLE, subtitleText);
                    }
                } else if (!playersShownSafeMessage.contains(playerId)) {
                    playersShownSafeMessage.add(playerId);

                    Component safeText = UtilMessage.deserialize("<green>Safe!");
                    player.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1000), Duration.ofMillis(500)));
                    player.sendTitlePart(TitlePart.SUBTITLE, safeText);
                } else {
                    if (playersShownSafeMessage.contains(playerId)) {
                        player.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1000), Duration.ofMillis(500)));
                        player.sendTitlePart(TitlePart.SUBTITLE, Component.empty());
                    }
                    playersShownSafeMessage.remove(playerId);
                }
            });
        }
    }

}
