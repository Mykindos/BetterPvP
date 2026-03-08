package me.mykindos.betterpvp.core.framework.economy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

@BPvPListener
@Singleton
public class CoinDeathListener implements Listener {

    @Inject
    @Config(path = "clans.coins.percentCoinsDroppedOnDeath", defaultValue = "0.10")
    private double percentCoinsDroppedOnDeath;

    @Inject
    @Config(path = "clans.coins.dropCoinsOnDeath", defaultValue = "true")
    private boolean dropCoinsOnDeath;

    private final ClientManager clientManager;

    @Inject
    public CoinDeathListener(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    public ItemStack generateDrops(int coinAmount) {
        if (coinAmount <= 0) {
            return null; //dont drop any coins
        }

        CoinItem coinItem = CoinItem.SMALL_NUGGET;

        if (coinAmount >= 20000) {
            coinItem = CoinItem.BAR;
        } else if (coinAmount >= 5000) {
            coinItem = CoinItem.LARGE_NUGGET;
        }

        return coinItem.generateItem(coinAmount);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!dropCoinsOnDeath) {
            return;
        }

        Player player = event.getPlayer();
        final Gamer gamer = clientManager.search().online(player).getGamer();

        // Fire the CoinDropEvent to allow runes and other modifiers to adjust the drop amount
        final CoinDropEvent coinDropEvent = UtilServer.callEvent(new CoinDropEvent(player, gamer.getBalance()));

        // Calculate the final drop amount including all bonuses
        final int dropAmount = coinDropEvent.getTotalDropAmount(percentCoinsDroppedOnDeath);
        gamer.saveProperty(GamerProperty.BALANCE, Math.max(0, gamer.getBalance() - dropAmount));
        UtilMessage.simpleMessage(player, "Death", "You lost <yellow>%s coins<gray> for dying.", UtilFormat.formatNumber(dropAmount));

        ItemStack coinItem = generateDrops(dropAmount);
        if (coinItem != null) {
            player.getWorld().dropItemNaturally(player.getLocation(), coinItem);
        }
    }
}
