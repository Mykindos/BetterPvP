package me.mykindos.betterpvp.clans.clans.loot;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.core.EnergyItem;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.ReplacementStrategy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

@EqualsAndHashCode(callSuper = true)
public final class GivenClanEnergyLoot extends ClanEnergyLoot<Map<Clan, Integer>> {

    GivenClanEnergyLoot(EnergyItem energyType, ReplacementStrategy replacementStrategy, Predicate<LootContext> condition, int minAmount, int maxAmount, boolean autoDeposit) {
        super(energyType, replacementStrategy, condition, minAmount, maxAmount, autoDeposit);
    }

    @Override
    public Map<Clan, Integer> award(LootContext context) {
        ClanManager clanManager = JavaPlugin.getPlugin(Clans.class).getInjector().getInstance(ClanManager.class);
        Map<Clan, Integer> results = new HashMap<>();
        context.getSession().getAudience().forEachAudience(audience -> {
            if (!(audience instanceof Player player)) {
                return;
            }
            clanManager.getClanByPlayer(player).ifPresent(clan -> {
                int amount = ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1);
                results.merge(clan, amount, Integer::sum);
                clan.grantEnergy(player, amount, context.getSource());
            });
        });
        return results;
    }
}
