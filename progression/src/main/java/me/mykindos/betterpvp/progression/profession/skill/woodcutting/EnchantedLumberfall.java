package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

@Singleton
@BPvPListener
public class EnchantedLumberfall extends WoodcuttingProgressionSkill implements Listener {
    private final ProfessionProfileManager professionProfileManager;
    private final ItemHandler itemHandler;

    @Inject
    public EnchantedLumberfall(Progression progression, ProfessionProfileManager professionProfileManager, ItemHandler itemHandler) {
        super(progression);
        this.professionProfileManager = professionProfileManager;
        this.itemHandler = itemHandler;
    }

    @Override
    public String getName() {
        return "Enchanted Lumberfall";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[] {
                "Whenever you fell a tree, there's a <green>" + specialItemDropChance(level) + "% chance to drop a special item"
        };
    }

    @Override
    public Material getIcon() {
        return Material.AZALEA_LEAVES;
    }

    /**
     * @param level the player's skill level
     * @return the computed special drop item chance that triggers whenever a player fells a tree
     */
    public double specialItemDropChance(int level) {
        return level*2;
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

    public void whenSkillTriggers(Player player, Location locationToDropItem) {

        World world = player.getWorld();

        UtilServer.runTaskLater(this.getProgression(), () -> {
            world.getBlockAt(locationToDropItem).breakNaturally();
            world.spawnParticle(Particle.CHERRY_LEAVES, locationToDropItem, 5);
            UtilItem.insert(player, itemHandler.updateNames(new ItemStack(Material.NETHERITE_AXE)));
        }, 20L);
    }
}
