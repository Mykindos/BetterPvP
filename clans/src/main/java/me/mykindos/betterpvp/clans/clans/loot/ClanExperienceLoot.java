package me.mykindos.betterpvp.clans.clans.loot;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.loot.Loot;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.ReplacementStrategy;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

@EqualsAndHashCode(callSuper = true)
public final class ClanExperienceLoot extends Loot<Long, Map<Player, Long>> {

    private final long minXp;
    private final long maxXp;

    public ClanExperienceLoot(ReplacementStrategy replacementStrategy, Predicate<LootContext> condition, long minXp, long maxXp) {
        super(replacementStrategy, condition);
        this.minXp = minXp;
        this.maxXp = maxXp;
    }

    /** Returns the max XP value, used for display purposes. */
    @Override
    public Long getReward() {
        return maxXp;
    }

    /**
     * @return The XP amount rolled for this award.
     */
    @Override
    public Map<Player, Long> award(LootContext context) {
        final Map<Player, Long> results = new HashMap<>();
        ClanManager clanManager = JavaPlugin.getPlugin(Clans.class).getInjector().getInstance(ClanManager.class);
        context.getSession().getAudience().forEachAudience(audience -> {
            if (audience instanceof Player player) {
                clanManager.getClanByPlayer(player).ifPresent(clan -> {
                    long xp = (minXp >= maxXp) ? minXp : ThreadLocalRandom.current().nextLong(minXp, maxXp);
                    clan.getExperience().grantXp(player, xp, context.getSource());
                });
            }
        });
        return results;
    }

    @Override
    public ItemView getIcon() {
        return ItemView.builder()
                .material(Material.EXPERIENCE_BOTTLE)
                .displayName(Component.text("Clan XP", NamedTextColor.GREEN))
                .build();
    }

    @Override
    public String toString() {
        return "ClanExperienceLoot{min=" + minXp + ", max=" + maxXp + "}";
    }
}
