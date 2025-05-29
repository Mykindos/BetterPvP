package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.common.base.Preconditions;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkillNode;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkillNode;
import me.mykindos.betterpvp.progression.profession.woodcutting.WoodcuttingHandler;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerStripLogEvent;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

/**
 * This perk grants a chance for the player to receive a <i>special item</i> when they strip a log.
 * <p>
 * This item is <b>Tree Bark</b> which can sold at the shops
 * </p>
 */
@Singleton
@BPvPListener
public class BarkBounty extends ProfessionSkillNode implements Listener {

    private final ProfessionProfileManager professionProfileManager;
    private final WoodcuttingHandler woodcuttingHandler;
    private final BlockTagManager blockTagManager;
    private final ItemFactory itemFactory;

    /**
     * Represents the percentage, per level, that <b>Tree Bark</b> will drop when any given log is stripped
     */
    private double barkChanceIncreasePerLvl;

    @Inject
    public BarkBounty(ProfessionProfileManager professionProfileManager, WoodcuttingHandler woodcuttingHandler, BlockTagManager blockTagManager,  ItemFactory itemFactory) {
        super("Bark Bounty");
        this.professionProfileManager = professionProfileManager;
        this.woodcuttingHandler = woodcuttingHandler;
        this.blockTagManager = blockTagManager;
        this.itemFactory = itemFactory;
    }

    @Override
    public String getName() {
        return "Bark Bounty";
    }

    @Override
    public String[] getDescription(int level) {
        double numberInPercentage = getChanceForBarkToDrop(level) * 100;
        String formattedNumber = UtilFormat.formatNumber(numberInPercentage, 2);

        return new String[]{
                "When you strip a log, there is <green>" + formattedNumber + "%</green> to drop <aqua>Tree Bark</aqua>",
                "",
                "<aqua>Tree Bark</aqua> can be used to purchase items from the Lumberjack at shops"
        };
    }

    @Override
    public Material getIcon() {
        return Material.STRIPPED_OAK_LOG;
    }

    @Override
    public void loadConfig() {
        super.loadConfig();
        barkChanceIncreasePerLvl = getConfig("barkChanceIncreasePerLvl", 0.004, Double.class);
    }

    /**
     * Calculates the probability that Tree Bark will drop when a log is stripped
     * <p>
     * 0.0 < the value that is returned <= 1.0
     */
    public double getChanceForBarkToDrop(int level) {
        return barkChanceIncreasePerLvl * level;
    }

    /**
     * Whenever a player strips a log, the logic in this method <i>should</i> be executed, and this method will
     * get a random number to see if the player should receive <b>Tree Bark</b> based on their level
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void whenPlayerStripsALog(PlayerStripLogEvent event) {
        if (event.wasEventDeniedAndCancelled()) return;
        final BaseItem treeBark = itemFactory.getItemRegistry().getItem("progression:tree_bark");
        Preconditions.checkState(treeBark != null, "Tree Bark item has not been registered!");

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.ADVENTURE) return;
        if (!player.getWorld().getName().equalsIgnoreCase(BPvPWorld.MAIN_WORLD_NAME)) return;

        Block block = event.getStrippedLog();
        if (blockTagManager.isPlayerPlaced(block)) return;

        professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            int skillLevel = getPlayerNodeLevel(profile);
            if (skillLevel <= 0) return;

            if (Math.random() < getChanceForBarkToDrop(skillLevel)) {
                final ItemStack itemStack = itemFactory.create(treeBark).createItemStack();
                final Location location = player.getEyeLocation().add(player.getLocation().getDirection().multiply(1));
                final Item item = player.getWorld().dropItem(location, itemStack);
                item.setVelocity(player.getLocation().getDirection().multiply(-0.2));
            }
        });
    }
}
