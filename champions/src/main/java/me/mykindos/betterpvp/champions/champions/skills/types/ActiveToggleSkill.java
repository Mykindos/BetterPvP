package me.mykindos.betterpvp.champions.champions.skills.types;

import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.effects.events.EffectReceiveEvent;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public abstract class ActiveToggleSkill extends Skill implements ToggleSkill, Listener {

    @Getter
    protected final Set<UUID> active = new HashSet<>();

    @Getter
    protected final HashMap<UUID, HashMap<String, Long>> updaterCooldowns = new HashMap<>();

    public ActiveToggleSkill(Champions champions, ChampionsManager championsManager) {
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

    @EventHandler
    public void onCustomEffect(EffectReceiveEvent event) {
        if (!(event.getTarget() instanceof Player player)) return;
        if (!active.contains(player.getUniqueId())) return;
        if (!hasSkill(player)) return;
        int level = getLevel(player);
        if (!canUseWhileSilenced() && (event.getEffect().getEffectType() == EffectType.SILENCE)) {
            UtilMessage.message(player, "Champions", UtilMessage.deserialize("<green>%s %s</green> was cancelled because you were <white>silenced</white>.", getName(), level));
            cancel(player);
        }
        if (!canUseWhileLevitating() && (event.getEffect().getEffectType() == EffectType.LEVITATION)) {
            UtilMessage.message(player, "Champions", UtilMessage.deserialize("<green>%s %s</green> was cancelled because you are now <white>levitating</white>.", getName(), level));
            cancel(player);
        }
        if (!canUseWhileStunned() && (event.getEffect().getEffectType() == EffectType.STUN)) {
            UtilMessage.message(player, "Champions", UtilMessage.deserialize("<green>%s %s</green> was cancelled because you were <white>stunned</white>.", getName(), level));
            cancel(player);
        }

    }

    @EventHandler
    public void onEnterWater(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (active.contains(player.getUniqueId()) && UtilBlock.isInWater(player) && !canUseInLiquid()) {
            cancel(event.getPlayer());
        }
    }

    @Override
    public void toggle(Player player, int level) {
        if (active.contains(player.getUniqueId())) {
            cancel(player);
        } else {
            active.add(player.getUniqueId());
            updaterCooldowns.put(player.getUniqueId(), new HashMap<>());
            toggleActive(player);
        }
    }

    public abstract boolean process(Player player);

    public abstract void toggleActive(Player player);


}
