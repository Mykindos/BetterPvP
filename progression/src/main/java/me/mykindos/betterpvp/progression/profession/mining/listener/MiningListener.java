package me.mykindos.betterpvp.progression.profession.mining.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.profession.mining.MiningHandler;
import me.mykindos.betterpvp.progression.profession.mining.event.PlayerMinesOreEvent;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

@BPvPListener
@CustomLog
@Singleton
public class MiningListener implements Listener {

    private final ClientManager clientManager;
    private final MiningHandler miningHandler;

    @Inject
    public MiningListener(ClientManager clientManager, MiningHandler miningHandler) {
        this.clientManager = clientManager;
        this.miningHandler = miningHandler;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        ItemStack toolUsed = event.getPlayer().getInventory().getItemInMainHand();
        Block minedBlock = event.getBlock();

        if (!UtilBlock.isOre(minedBlock.getType())) return;
        Client client = clientManager.search().online(event.getPlayer());
        if (client.isAdministrating() || event.getPlayer().getGameMode().isInvulnerable()) return;

        PlayerMinesOreEvent minesOreEvent = UtilServer.callEvent(
                new PlayerMinesOreEvent(event.getPlayer(), minedBlock, toolUsed));

        if (minesOreEvent.isCancelled()) return;

        if (minesOreEvent.isSmelted()) {
            event.setDropItems(false);
            int amount = minesOreEvent.isDoubledDrops() ? minesOreEvent.getSmeltedAmount() * 2 : minesOreEvent.getSmeltedAmount();
            minedBlock.getWorld().dropItemNaturally(minedBlock.getLocation(), new ItemStack(minesOreEvent.getSmeltedItem(), amount));
        } else {
            event.setDropItems(false);
            int amount = minesOreEvent.isDoubledDrops() ? 2 : 1;
            Collection<ItemStack> drops = minedBlock.getDrops(event.getPlayer().getInventory().getItemInMainHand());
            for (ItemStack drop : drops) {
                drop.setAmount(drop.getAmount() * amount);
                minedBlock.getWorld().dropItemNaturally(minedBlock.getLocation(), drop);
            }
        }

        miningHandler.attemptMineOre(event.getPlayer(), event.getBlock());
    }
}
