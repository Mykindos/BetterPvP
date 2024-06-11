package me.mykindos.betterpvp.champions.champions.skills.types;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.effects.events.EffectReceiveEvent;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public abstract class ChannelSkill extends Skill implements Listener {

    protected final Set<UUID> active = new HashSet<>();
    public ChannelSkill(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        cancel(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer());
    }

    public void cancel(Player player) {
        active.remove(player.getUniqueId());
    }
    @EventHandler (priority = EventPriority.MONITOR)
    public void onCustomEffect(EffectReceiveEvent event) {
        if(event.isCancelled()) return;
        if ((event.getTarget() instanceof Player player)) {
            if (!canUseWhileSilenced() && (event.getEffect().getEffectType() == EffectTypes.SILENCE)) {
                cancel(player);
            }
            if (!canUseWhileLevitating() && (event.getEffect().getEffectType() == EffectTypes.LEVITATION)) {
                cancel(player);
            }
            if (!canUseWhileStunned() && (event.getEffect().getEffectType() == EffectTypes.STUN)) {
                cancel(player);
            }
        }
    }

    @EventHandler
    public void onEnterWater(PlayerMoveEvent event) {
        if(!event.hasChangedPosition()) return;

        if (UtilBlock.isInWater(event.getPlayer()) && !canUseInLiquid()) {
            cancel(event.getPlayer());
        }
    }

    public boolean isShieldInvisible() {
        return true;
    }

    public boolean shouldShowShield(Player player) {
        return true;
    }
}
