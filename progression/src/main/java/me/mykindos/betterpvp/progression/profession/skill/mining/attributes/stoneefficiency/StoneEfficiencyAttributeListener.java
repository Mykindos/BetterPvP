package me.mykindos.betterpvp.progression.profession.skill.mining.attributes.stoneefficiency;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.blockbreak.global.GlobalBlockBreakRules;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockBreakProperties;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockBreakRule;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockMatcher;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.RuleLayer;
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
     * Multiplicative rule — properties are computed per-resolve so level-ups apply
     * without re-registering. Quits clear all of a player's rules via
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
            final double bonus = player == null ? 0.0 : attribute.getMiningSpeedBonus(player);
            return BlockBreakProperties.multiplier(1.0 + Math.max(0.0, bonus));
        }

        @Override
        public @NotNull Predicate<Player> condition() {
            return p -> p.getUniqueId().equals(playerId) && attribute.getMiningSpeedBonus(p) > 0;
        }

        @Override
        public @NotNull RuleLayer layer() {
            return RuleLayer.MULTIPLICATIVE;
        }
    }
}
