package me.mykindos.betterpvp.core.loot.economy;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.economy.CoinItem;
import me.mykindos.betterpvp.core.framework.economy.CoinPickupListener;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.ReplacementStrategy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

@EqualsAndHashCode(callSuper = true)
public final class GivenCoinLoot extends CoinLoot<Map<Player, Integer>> {

    GivenCoinLoot(CoinItem coinType, ReplacementStrategy replacementStrategy, Predicate<LootContext> condition, int minAmount, int maxAmount) {
        super(coinType, replacementStrategy, condition, minAmount, maxAmount);
    }

    @Override
    public Map<Player, Integer> award(LootContext context) {
        Map<Player, Integer> results = new HashMap<>();
        ClientManager clientManager = JavaPlugin.getPlugin(Core.class).getInjector().getInstance(ClientManager.class);
        context.getSession().getAudience().forEachAudience(audience -> {
            if (!(audience instanceof Player player)) {
                return;
            }

            final Gamer gamer = clientManager.search().online(player).getGamer();
            int amount = (minAmount == maxAmount) ? minAmount : ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1);
            int newBalance = gamer.getBalance() + amount;
            gamer.saveProperty(GamerProperty.BALANCE, newBalance);
            CoinPickupListener.notify(gamer, amount);
            results.merge(player, amount, Integer::sum);
        });
        return results;
    }
}
