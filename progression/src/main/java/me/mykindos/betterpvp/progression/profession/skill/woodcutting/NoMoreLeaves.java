package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.woodcutting.WoodcuttingHandler;
import me.mykindos.betterpvp.progression.profession.woodcutting.WoodcuttingLoot;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

/**
 * This skill is causes leaves to be removed when the player uses <b>Tree Feller</b>.
 * <br />
 * Additionally, it grants the player a chance to receive special drops from the removed leaves.
 */
@Singleton
@Getter
public class NoMoreLeaves extends WoodcuttingProgressionSkill {
    private final ProfessionProfileManager professionProfileManager;
    private final WoodcuttingHandler woodcuttingHandler;

    private int baseMaxLeavesCount;
    private double leavesCountIncreasePerLvl;
    private double specialDropChanceIncreasePerLvl;

    /**
     * Global Map to prevent the player from chopping off too many leaves from a felled tree
     */
    public final HashMap<UUID, Integer> felledTreeLeavesMap = new HashMap<>();


    @Inject
    public NoMoreLeaves(Progression progression, ProfessionProfileManager professionProfileManager, WoodcuttingHandler woodcuttingHandler) {
        super(progression);
        this.professionProfileManager = professionProfileManager;
        this.woodcuttingHandler = woodcuttingHandler;
    }

    @Override
    public String getName() {
        return "No More Leaves";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[] {
                "Removes leaves when <green>Tree Feller</green> is used",
                "Grants a <green>" + specialDropChance(level) * 100 + "%</green> chance for rarer drops from the removed leaves",
                "",
                "Removable Leaves Cap (increases by 1 leaf block per 2 levels): <green>" + maxLeavesCount(level),
        };
    }

    @Override
    public Material getIcon() {
        return Material.AZALEA_LEAVES;
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

    /**
     * @return Gets a random loot from WoodcuttingHandler
    public WoodcuttingLoot getRandomLoot() {
        return woodcuttingHandler.getLootTypes().random();
    }

    /**
     * @return the calculated max count of leaves that can be removed based on player's level
     */
    public int maxLeavesCount(int level) {
        return (int) (baseMaxLeavesCount + Math.floor(leavesCountIncreasePerLvl*level));
    }

    /**
     * @return the chance of getting a rarer drop from leaves (i.e. loot)
     */
    public double specialDropChance(int level) {
        return level * specialDropChanceIncreasePerLvl;
    }

    @Override
    public void loadConfig() {
        super.loadConfig();
        baseMaxLeavesCount = getConfig("baseMaxLeavesCount", 10, Integer.class);
        leavesCountIncreasePerLvl = getConfig("leavesCountIncreasePerLvl", 0.5, Double.class);
        specialDropChanceIncreasePerLvl = getConfig("specialDropChanceIncreasePerLvl", 0.007, Double.class);
    }
}
