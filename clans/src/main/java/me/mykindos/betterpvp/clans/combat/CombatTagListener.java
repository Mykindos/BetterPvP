package me.mykindos.betterpvp.clans.combat;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.model.display.TitleComponent;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

@BPvPListener
public class CombatTagListener implements Listener {
    private final ClientManager clientManager;
    private final EffectManager effectManager;
    private final ClanManager clanManager;

    private final Set<LivingEntity> combatNotified = new HashSet<>();

    @Inject
    public CombatTagListener(ClientManager clientManager, EffectManager effectManager, ClanManager clanManager) {
        this.clientManager = clientManager;
        this.effectManager = effectManager;
        this.clanManager = clanManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void notifyPlayersInCombat(CustomDamageEvent event) {
        if (!(event.getDamagee() instanceof Player damagee)) {
            return;
        }

        LivingEntity damager = event.getDamager();

        if (!combatNotified.contains(damagee)) {
            combatNotified.add(damagee);
            Component combatMessage = Component.text("You are now in combat!", NamedTextColor.RED);
            UtilMessage.message(damagee, "Combat", combatMessage);
        }

        if (damager instanceof Player damagerPlayer && !combatNotified.contains(damagerPlayer)) {
            combatNotified.add(damagerPlayer);
            Component combatMessage = Component.text("You are now in combat!", NamedTextColor.RED);
            UtilMessage.message(damagerPlayer, "Combat", combatMessage);
        }
    }


    @UpdateEvent(delay = 500)
    public void showTag() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            final Gamer gamer = clientManager.search().online(player).getGamer();

            if (gamer.isInCombat()) {
                if (effectManager.hasEffect(player, EffectTypes.VANISH)) return;

                if (!clanManager.isInSafeZone(player)) return;

                Random rand = UtilMath.RANDOM;

                for (int i = 0; i < 10; i++) {
                    double offsetX = (rand.nextDouble() * 2 - 1) * 0.25;
                    double offsetY = (rand.nextDouble() * 2 - 1) * 0.25;
                    double offsetZ = (rand.nextDouble() * 2 - 1) * 0.25;

                    Particle.CRIT.builder()
                            .location(player.getLocation().add(0, 2.25, 0).add(offsetX, offsetY, offsetZ))
                            .extra(0).receivers(30).spawn();
                }
            } else if (combatNotified.contains(player)) {
                long timeSinceLastDamage = System.currentTimeMillis() - gamer.getLastDamaged();
                if (timeSinceLastDamage > 15000) {
                    combatNotified.remove(player);
                    Component safeMessage = Component.text("You are no longer in combat.", NamedTextColor.GREEN);
                    UtilMessage.message(player, "Combat", safeMessage);
                }
            }
        }
    }

    @UpdateEvent
    public void showSafetySubtitle() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            Gamer gamer = clientManager.search().online(player).getGamer();

            if (gamer.isInCombat()) {
                if (clanManager.isInSafeZone(player)) {

                    long remainingMillis = 15000 - (System.currentTimeMillis() - gamer.getLastDamaged());
                    double remainingSeconds = remainingMillis / 1000.0;

                    Component subtitleText = Component.text("Unsafe for: ", NamedTextColor.GRAY).append(Component.text(String.format("%.1f", remainingSeconds) + "s", NamedTextColor.RED));
                    TitleComponent titleComponent = new TitleComponent(0, 0.4, 0, false,
                            g -> Component.text("", NamedTextColor.GRAY),
                            g -> subtitleText);

                    gamer.getTitleQueue().add(10, titleComponent);

                }
            }
        }
    }
}
