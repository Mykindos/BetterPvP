package me.mykindos.betterpvp.progression.profession.skill.mining.attributes.stoneefficiency;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.blockbreak.ToolMiningSpeed;
import me.mykindos.betterpvp.core.framework.blockbreak.global.GlobalBlockBreakRules;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockBreakProperties;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockBreakRule;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockMatcher;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.preset.BlockGroups;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.Predicate;

@BPvPListener
@Singleton
public class StoneEfficiencyAttributeListener implements Listener {

    private final StoneEfficiencyAttribute attribute;
    private final GlobalBlockBreakRules globalRules;

    @Inject
    public StoneEfficiencyAttributeListener(StoneEfficiencyAttribute attribute, GlobalBlockBreakRules globalRules) {
        this.attribute = attribute;
        this.globalRules = globalRules;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        globalRules.addRule(event.getPlayer().getUniqueId(),
                new DynamicStoneEfficiencyRule(event.getPlayer().getUniqueId(), attribute));
    }

    /**
     * Properties are computed per-resolve from the live attribute value, so level-ups
     * apply without re-registering the rule. Quits clear all of a player's rules via
     * {@code GlobalBlockBreakRulesImpl#onQuit}.
     */
    private record DynamicStoneEfficiencyRule(UUID playerId, StoneEfficiencyAttribute attribute) implements BlockBreakRule {

        @Override
        public @NotNull BlockMatcher matcher() {
            return BlockGroups.STONES;
        }

        @Override
        public @NotNull BlockBreakProperties properties() {
            final Player player = Bukkit.getPlayer(playerId);
            if (player == null) return BlockBreakProperties.breakable(BlockBreakProperties.MIN_SPEED);
            final double bonus = attribute.getMiningSpeedBonus(player);
            final int speed = Math.max(BlockBreakProperties.MIN_SPEED, (int) Math.round(ToolMiningSpeed.DIAMOND * bonus));
            return BlockBreakProperties.breakable(speed);
        }

        @Override
        public @NotNull Predicate<Player> condition() {
            return p -> p.getUniqueId().equals(playerId) && attribute.getMiningSpeedBonus(p) > 0;
        }
    }
}
