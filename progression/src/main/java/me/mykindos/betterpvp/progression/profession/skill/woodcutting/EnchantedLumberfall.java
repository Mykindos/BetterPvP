package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;

@Singleton
@BPvPListener
public class EnchantedLumberfall extends WoodcuttingProgressionSkill implements Listener {
    private final ProfessionProfileManager professionProfileManager;

    @Inject
    public EnchantedLumberfall(Progression progression, ProfessionProfileManager professionProfileManager) {
        super(progression);
        this.professionProfileManager = professionProfileManager;
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

    public void whenSkillTriggers(Player player, ImmutableSet<Location> leafLocations) {
        player.sendMessage("Enchanted lumberfall!");

        if (leafLocations.isEmpty()) {
            player.sendMessage("No leaves attached to this tree at all!");
            return;
        }

        player.sendMessage("Found some leaves!");

        OptionalInt lowestLeafYValue = leafLocations.stream()
                .mapToInt(Location::getBlockY)
                .min();

        List<Location> lowestLeafLocations = leafLocations.stream()
                .filter(location -> location.getBlockY() == lowestLeafYValue.getAsInt())
                .toList();

        Random random = new Random();
        Location randomLocation = lowestLeafLocations.get(
                random.nextInt(lowestLeafLocations.size())
        );

        player.getWorld().dropItem(randomLocation, new ItemStack(Material.DIAMOND));


    }
}
