package me.mykindos.betterpvp.core.client.stats.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.impl.core.item.ItemStat;
import me.mykindos.betterpvp.core.components.shops.events.FinalPlayerBuyItemEvent;
import me.mykindos.betterpvp.core.components.shops.events.FinalPlayerSellItemEvent;
import me.mykindos.betterpvp.core.item.attunement.PlayerAttuneItemEvent;
import me.mykindos.betterpvp.core.item.reforging.PlayerReforgeItemEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@BPvPListener
public class ItemStatListener implements Listener {
    private final ClientManager clientManager;

    @Inject
    public ItemStatListener(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onItemReforge(PlayerReforgeItemEvent event) {
        //todo should we track the augmentation component?
        final ItemStat itemStat = ItemStat.builder()
                .itemStack(event.getItemInstance().getItemStack())
                .action(ItemStat.Action.REFORGE)
                .build();
        clientManager.incrementStat(event.getPlayer(), itemStat, 1L);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onItemAttune(PlayerAttuneItemEvent event) {
        //todo should we track the purity component?
        final ItemStat itemStat = ItemStat.builder()
                .itemStack(event.getItemInstance().getItemStack())
                .action(ItemStat.Action.ATTUNE)
                .build();
        clientManager.incrementStat(event.getPlayer(), itemStat, 1L);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerBuy(final FinalPlayerBuyItemEvent event) {
        final ItemStat countStat = ItemStat.builder()
                .itemStack(event.getItem())
                .action(ItemStat.Action.BUY_COUNT)
                .build();
        final ItemStat amountStat = ItemStat.builder()
                .itemStack(event.getItem())
                .action(ItemStat.Action.BUY_AMOUNT)
                .build();
        clientManager.incrementStat(event.getPlayer(), countStat, event.getCount());
        clientManager.incrementStat(event.getPlayer(), amountStat, event.getTotalAmount());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerSell(final FinalPlayerSellItemEvent event) {
        final ItemStat countStat = ItemStat.builder()
                .itemStack(event.getItem())
                .action(ItemStat.Action.SELL_COUNT)
                .build();
        final ItemStat amountStat = ItemStat.builder()
                .itemStack(event.getItem())
                .action(ItemStat.Action.SELL_AMOUNT)
                .build();
        clientManager.incrementStat(event.getPlayer(), countStat, event.getCount());
        clientManager.incrementStat(event.getPlayer(), amountStat, event.getTotalAmount());
    }
}
