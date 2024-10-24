package me.mykindos.betterpvp.clans.clans.coins;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.core.EnergyItem;
import me.mykindos.betterpvp.clans.fields.model.FieldsBlock;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@BPvPListener
@Singleton
class CoinDeathListener implements Listener {

    @Inject
    ClientManager clientManager;

    @Inject
    @Config(path = "clans.coins.percentCoinsDroppedOnDeath", defaultValue = "0.10")
    private double percentCoinsDroppedOnDeath;

    @Inject
    @Config(path = "clans.coins.dropCoinsOnDeath", defaultValue = "true")
    private boolean dropCoinsOnDeath;


    public ItemStack generateDrops(int coinAmount) {
        if (coinAmount <= 0) {
            return null; //dont drop any coins
        }

        CoinItem coinItem;

        if (coinAmount >= 5000) {
            coinItem = CoinItem.CUBE;
        } else if (coinAmount >= 1000) {
            coinItem = CoinItem.BAR;
        } else {
            coinItem = CoinItem.NUGGET;
        }

        return coinItem.generateItem(coinAmount);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!dropCoinsOnDeath){
            return;
        }

        Player player = event.getPlayer();
        final Gamer gamer = clientManager.search().online(player).getGamer();

        final int dropAmount = (int)(gamer.getBalance() * percentCoinsDroppedOnDeath);
        gamer.saveProperty(GamerProperty.BALANCE, gamer.getBalance() - dropAmount);

        ItemStack coinItem = generateDrops(dropAmount);
        if (coinItem != null) {
            player.getWorld().dropItemNaturally(player.getLocation(), coinItem);
        }
    }
}
