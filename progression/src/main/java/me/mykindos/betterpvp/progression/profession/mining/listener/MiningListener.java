package me.mykindos.betterpvp.progression.profession.mining.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.profession.mining.MiningHandler;
import me.mykindos.betterpvp.progression.profession.mining.event.PlayerMinesOreEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
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
    private final EffectManager effectManager;

    @Inject
    public MiningListener(ClientManager clientManager, MiningHandler miningHandler, EffectManager effectManager) {
        this.clientManager = clientManager;
        this.miningHandler = miningHandler;
        this.effectManager = effectManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        ItemStack toolUsed = event.getPlayer().getInventory().getItemInMainHand();
        Block minedBlock = event.getBlock();

        Client client = clientManager.search().online(event.getPlayer());
        if (client.isAdministrating() || event.getPlayer().getGameMode().isInvulnerable()) return;

        if (UtilBlock.isOre(minedBlock.getType())) {
            PlayerMinesOreEvent minesOreEvent = UtilServer.callEvent(
                    new PlayerMinesOreEvent(event.getPlayer(), minedBlock, toolUsed));

            if (minesOreEvent.isCancelled()) return;
            if (minesOreEvent.isSmelted()) {
                event.setDropItems(false);
                int amount = minesOreEvent.isDoubledDrops() ? minesOreEvent.getSmeltedAmount() * 2 : minesOreEvent.getSmeltedAmount();
                Item item = minedBlock.getWorld().dropItemNaturally(minedBlock.getLocation(), new ItemStack(minesOreEvent.getSmeltedItem(), amount));
                if (effectManager.hasEffect(event.getPlayer(), EffectTypes.PROTECTION)) {
                    UtilItem.reserveItem(item, event.getPlayer(), 10.0);
                }
            } else {
                event.setDropItems(false);
                int amount = minesOreEvent.isDoubledDrops() ? 2 : 1;
                Collection<ItemStack> drops = minedBlock.getDrops(event.getPlayer().getInventory().getItemInMainHand());
                for (ItemStack drop : drops) {
                    drop.setAmount(drop.getAmount() * amount);
                    Item item = minedBlock.getWorld().dropItemNaturally(minedBlock.getLocation(), drop);
                    if (effectManager.hasEffect(event.getPlayer(), EffectTypes.PROTECTION)) {
                        UtilItem.reserveItem(item, event.getPlayer(), 10.0);
                    }

                }
            }
        }

        miningHandler.attemptMineOre(event.getPlayer(), event.getBlock());
    }
}
