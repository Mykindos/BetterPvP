package me.mykindos.betterpvp.progression.profession.skill.woodcutting.treecompactor;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkill;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Optional;

@Singleton
@NodeId("tree_compactor")
public class TreeCompactor extends ProfessionSkill {

    private final ProfessionProfileManager profileManager;
    private final ClientManager clientManager;
    private final ItemFactory itemFactory;
    private final ItemRegistry itemRegistry;

    @Getter
    private double cooldown;

    @Inject
    public TreeCompactor(ProfessionProfileManager profileManager,
                         ClientManager clientManager, ItemFactory itemFactory, ItemRegistry itemRegistry) {
        super("Tree Compactor");
        this.profileManager = profileManager;
        this.clientManager = clientManager;
        this.itemFactory = itemFactory;
        this.itemRegistry = itemRegistry;
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "You gain access to the <green>/treecompactor</green> command",
                "",
                "This command lets you turn a stack of logs",
                "into a singular log"
        };
    }

    @Override
    public Material getIcon() {
        return Material.OAK_WOOD;
    }

    @Override
    public void loadSkillConfig() {
        
        cooldown = getSkillConfig("cooldown", 20.0, Double.class);
    }

    public void onPlaceCompactedLog(BlockPlaceEvent event) {
        Optional<ItemInstance> compactedLog = itemFactory.fromItemStack(event.getItemInHand());
        if (compactedLog.isEmpty()) {
            return;
        }

        final NamespacedKey key = itemRegistry.getKey(compactedLog.get().getBaseItem());
        if (key == null) {
            return; // Vanilla item or not registered
        }

        if  (!key.toString().equals("progression:compacted_log")) {
            return; // Not a compacted log
        }

        Player player = event.getPlayer();
        Client client = clientManager.search().online(player);
        if (!client.isAdministrating()) {
            event.setCancelled(true);
            UtilMessage.simpleMessage(player, "Progression", "You cannot place this block");
        }
    }

    /**
     * cancel crafting compacted logs into wooden planks
     */

    public void onCompactedLogCraft(CraftItemEvent event) {
        ItemStack result = event.getRecipe().getResult();
        if (result.getAmount() != 4 && result.getType() != Material.OAK_PLANKS) return;

        final BaseItem compactedLogItem = itemRegistry.getItem(new NamespacedKey("progression", "compacted_log"));
        Preconditions.checkNotNull(compactedLogItem, "Compacted log item must not be null");

        Arrays.stream(event.getInventory().getMatrix()).forEach(itemStack -> {
            if (itemStack == null || itemStack.getType() == Material.AIR) return;

            final ItemInstance instance = itemFactory.fromItemStack(itemStack).orElseThrow();
            if (instance.getBaseItem() != compactedLogItem) return;
            event.setResult(Event.Result.DENY);
        });
    }

}
