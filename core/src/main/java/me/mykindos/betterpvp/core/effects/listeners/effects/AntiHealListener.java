package me.mykindos.betterpvp.core.effects.listeners.effects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.effects.events.EffectReceiveEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

@BPvPListener
@Singleton
public class AntiHealListener implements Listener {

    private final EffectManager effectManager;

    @Inject
    public AntiHealListener(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @EventHandler
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof LivingEntity ent)) return;
        effectManager.getEffect(ent, EffectTypes.ANTI_HEAL).ifPresent(effect -> {
            if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED
                    || event.getRegainReason() == EntityRegainHealthEvent.RegainReason.REGEN
                    || event.getRegainReason() == EntityRegainHealthEvent.RegainReason.MAGIC_REGEN) {
                if ((event.getEntity() instanceof Player player)) {
                    player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.5f, 2.0f);
                }
                event.setCancelled(true);
            }
        });
    }

    @EventHandler
    public void onReceiveEffect(EffectReceiveEvent event) {
        if (event.isCancelled()) return;
        if (event.getEffect().getEffectType() == EffectTypes.ANTI_HEAL) {
            event.getTarget().getWorld().playSound(event.getTarget().getLocation(), Sound.BLOCK_GLASS_BREAK, 2.0f, 2.0f );
            UtilMessage.message(event.getTarget(), "Anti Heal", "You can no longer regenerate health!");
        }
    }

    @EventHandler
    public void removeEffectOnDeath(PlayerDeathEvent event) {
        effectManager.removeEffect(event.getEntity(), EffectTypes.REGENERATION);
    }
}