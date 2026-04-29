package me.mykindos.betterpvp.progression.profession.skill.mining.attributes.salvagerstouch;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.loot.Loot;
import me.mykindos.betterpvp.core.loot.LootBundle;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.LootTable;
import me.mykindos.betterpvp.core.loot.LootTableRegistry;
import me.mykindos.betterpvp.core.loot.session.LootSession;
import me.mykindos.betterpvp.core.loot.session.LootSessionController;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.progression.profession.mining.event.PlayerMinesOreEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class SalvagersTouchAttributeListener implements Listener {

    private static final String LOOT_TABLE_ID = "mining_salvagers_touch";

    private final SalvagersTouchAttribute attribute;
    private final BlockTagManager blockTagManager;
    private final LootTableRegistry lootTableRegistry;
    private final LootSessionController sessionController;

    @Inject
    public SalvagersTouchAttributeListener(SalvagersTouchAttribute attribute, BlockTagManager blockTagManager,
                                           LootTableRegistry lootTableRegistry, LootSessionController sessionController) {
        this.attribute = attribute;
        this.blockTagManager = blockTagManager;
        this.lootTableRegistry = lootTableRegistry;
        this.sessionController = sessionController;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMinesOre(PlayerMinesOreEvent event) {
        Block block = event.getMinedOreBlock();
        if (!UtilBlock.isStoneBased(block)) return;
        if (UtilBlock.isOre(block.getType())) return;
        if (blockTagManager.isPlayerPlaced(block)) return;

        double chance = attribute.getChance(event.getPlayer());
        if (chance <= 0) return;
        if (Math.random() >= chance) return;

        Player player = event.getPlayer();
        LootTable table = lootTableRegistry.loadLootTable(LOOT_TABLE_ID);
        LootSession session = sessionController.resolve(player, table, () -> LootSession.newSession(table, player));
        LootContext context = new LootContext(session, block.getLocation(), "Mining");
        LootBundle bundle = table.generateLoot(context);
        for (Loot<?, ?> loot : bundle) {
            loot.award(context);
        }
    }
}
