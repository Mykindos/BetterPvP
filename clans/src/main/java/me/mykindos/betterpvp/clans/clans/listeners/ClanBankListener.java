package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.core.events.ClanCoreDestroyedEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.economy.CoinItem;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;

@BPvPListener
@Singleton
public class ClanBankListener extends ClanListener {

    @Inject
    @Config(path="clans.bank.interestIntervalInHours", defaultValue = "24")
    private int interestIntervalInHours;

    @Inject
    @Config(path="clans.bank.interestIntervalDeviation", defaultValue = "6")
    private int interestIntervalDeviation;

    @Inject
    @Config(path="clans.bank.interestRate", defaultValue = "2.5")
    private double interestRate;

    @Inject
    public ClanBankListener(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @EventHandler
    public void onClanCoreDestroyed(ClanCoreDestroyedEvent event) {
        int amount = event.getClan().getBalance();
        ItemStack coin = CoinItem.LARGE_NUGGET.generateItem(amount);
        Location position = event.getClan().getCore().getPosition();
        if (position == null) return;
        event.getClan().setBalance(0);
        position.getWorld().dropItemNaturally(position.add(0, 1, 0), coin);
    }

}
