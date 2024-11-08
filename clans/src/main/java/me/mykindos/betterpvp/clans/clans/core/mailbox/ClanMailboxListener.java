package me.mykindos.betterpvp.clans.clans.core.mailbox;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.core.events.ClanCoreDestroyedEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

@BPvPListener
@Singleton
public class ClanMailboxListener implements Listener {

    private final ClanManager clanManager;

    @Inject
    public ClanMailboxListener(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @EventHandler
    public void onClanCoreDestroyed(ClanCoreDestroyedEvent event) {
        Clan clan = event.getClan();

        Location position = clan.getCore().getPosition();
        if(position == null) return;

        Location dropLocation = position.clone().add(0, 1, 0);
        ClanMailbox mailbox = clan.getCore().getMailbox();

        for(ItemStack item : mailbox.getContents()) {
            dropLocation.getWorld().dropItem(dropLocation, item);
        }

        mailbox.getContents().clear();

    }

}
