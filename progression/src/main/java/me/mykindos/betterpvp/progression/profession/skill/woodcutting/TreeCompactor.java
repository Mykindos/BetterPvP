package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

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
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillDependency;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Optional;

@Singleton
@BPvPListener
public class TreeCompactor extends WoodcuttingProgressionSkill implements Listener {

    private final ProfessionProfileManager professionProfileManager;
    private final ClientManager clientManager;
    private final ItemFactory itemFactory;
    private final ItemRegistry itemRegistry;

    @Getter
    private double cooldown;

    @Inject
    public TreeCompactor(Progression progression, ProfessionProfileManager professionProfileManager,
                         ClientManager clientManager, ItemFactory itemFactory, ItemRegistry itemRegistry) {
        super(progression);
        this.professionProfileManager = professionProfileManager;
        this.clientManager = clientManager;
        this.itemFactory = itemFactory;
        this.itemRegistry = itemRegistry;
    }

    @Override
    public String getName() {
        return "Tree Compactor";
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
    public boolean isGlowing() {
        return true;
    }

    @Override
    public void loadConfig() {
        super.loadConfig();
        cooldown = getConfig("cooldown", 20.0, Double.class);
    }

    /**
     * @return the player's skill level
     */
    public int getPlayerSkillLevel(Player player) {
        Optional<ProfessionProfile> profile = professionProfileManager.getObject(player.getUniqueId().toString());

        return profile.map(this::getPlayerSkillLevel).orElse(0);
    }

    /**
     * This function's purpose is to return a boolean that tells you if the player has the skill
     * <b>No More Leaves</b>
     */
    public boolean doesPlayerHaveSkill(Player player) {
        return getPlayerSkillLevel(player) > 0;
    }

    @EventHandler
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

    @EventHandler
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

    @Override
    public ProgressionSkillDependency getDependencies() {
        final String[] dependencies = new String[]{"Tree Tactician", "Forest Flourisher", "Bark Bounty"};
        return new ProgressionSkillDependency(dependencies, 250);
    }
}
