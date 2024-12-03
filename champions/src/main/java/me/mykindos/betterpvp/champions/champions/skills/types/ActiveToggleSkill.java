package me.mykindos.betterpvp.champions.champions.skills.types;

import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.effects.events.EffectReceiveEvent;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
public abstract class ActiveToggleSkill extends Skill implements ToggleSkill, Listener {

    protected final Set<UUID> active = new HashSet<>();

    protected final HashMap<UUID, HashMap<String, Long>> updaterCooldowns = new HashMap<>();

    protected ActiveToggleSkill(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (active.contains(event.getPlayer().getUniqueId())) {
            cancel(event.getPlayer(), null);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer());
    }

    public void cancel(Player player) {
        cancel(player, null);
    }

    protected void cancel(Player player, String reason) {
        active.remove(player.getUniqueId());
        if (reason == null) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "%s: <red>Off", getName());
        } else {
            UtilMessage.simpleMessage(player, getClassType().getName(), "%s: <red>Off <reset>(<alt2>%s</alt2>)", getName(), reason);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCustomEffect(EffectReceiveEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getTarget() instanceof Player player)) return;
        if (!event.getEffect().getEffectType().isNegative()) return;
        if (!active.contains(player.getUniqueId())) return;
        if (!hasSkill(player)) return;

        if (!canUseWhileSilenced() && (event.getEffect().getEffectType() == EffectTypes.SILENCE)) {
            cancel(player, "Silenced");
        }
        if (!canUseWhileLevitating() && (event.getEffect().getEffectType() == EffectTypes.LEVITATION)) {
            cancel(player, "Levitating");
        }
        if (!canUseWhileStunned() && (event.getEffect().getEffectType() == EffectTypes.STUN)) {
            cancel(player, "Stunned");
        }

    }

    @EventHandler
    public void onEnterWater(PlayerMoveEvent event) {
        if (active.contains(event.getPlayer().getUniqueId()) && UtilBlock.isInWater(event.getPlayer()) && !canUseInLiquid()) {
            cancel(event.getPlayer(), "Water");
        }
    }

    @Override
    public void toggle(Player player, int level) {
        if (active.contains(player.getUniqueId())) {
            cancel(player, null);
        } else {
            active.add(player.getUniqueId());
            updaterCooldowns.put(player.getUniqueId(), new HashMap<>());
            toggleActive(player);
        }
    }

    public abstract boolean process(Player player);

    public abstract void toggleActive(Player player);

    public float getEnergyStartCost(int level) {
        return (float) (energyStartCost - ((level - 1) * energyStartCostDecreasePerLevel));
    }

    public boolean isActive(Player player) {
        return this.active.contains(player.getUniqueId());
    }

}
