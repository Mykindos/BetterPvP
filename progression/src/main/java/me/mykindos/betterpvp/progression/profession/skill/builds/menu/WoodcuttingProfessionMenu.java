package me.mykindos.betterpvp.progression.profession.skill.builds.menu;

import me.mykindos.betterpvp.progression.profession.skill.ProfessionNodeManager;
import me.mykindos.betterpvp.progression.profession.skill.builds.menu.tree.ConnectionType;
import me.mykindos.betterpvp.progression.profession.skill.builds.menu.tree.SkillNodeType;
import me.mykindos.betterpvp.progression.profession.woodcutting.WoodcuttingHandler;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;

import java.util.List;

public class WoodcuttingProfessionMenu extends ProfessionMenu {
    public WoodcuttingProfessionMenu(WoodcuttingHandler woodcuttingHandler, ProfessionProfile professionProfile, ProfessionNodeManager progressionSkillManager) {
        super("Woodcutting", woodcuttingHandler, professionProfile, progressionSkillManager);

        setContent(List.of(
                // Row 1
                AIR, AIR, getSkillItem("Bark Bounty"), AIR, getSkillItem("Tree Tactician"),
                AIR, getSkillItem("Forest Flourisher"), AIR, AIR,

                // Row 2
                AIR, AIR, getConnectionItem(ConnectionType.STRAIGHT_VERTICAL, "Auto Planter"), AIR, AIR, AIR, AIR, AIR, AIR,

                // Row 3
                AIR, AIR, getSkillItem("Auto Planter"), AIR, getSkillItem("Tree Feller"),
                AIR, getSkillItem("Tree Compactor"), AIR, AIR,

                // Row 4
                AIR, AIR, AIR, AIR, AIR, AIR, AIR, AIR, AIR,

                // Row 5
                AIR, AIR, AIR, AIR, getSkillItem("Enchanted Lumberfall"), AIR, AIR, AIR, AIR,

                // Row 6
                AIR, AIR, AIR, AIR, AIR, AIR, AIR, AIR, AIR,

                // Row 7
                AIR, AIR, AIR, AIR, AIR, AIR, AIR, AIR, AIR,

                // Row 8
                AIR, AIR, AIR, AIR, AIR, AIR, AIR, AIR, AIR,

                // Row 9
                AIR, AIR, AIR, AIR, AIR, AIR, AIR, AIR, AIR
        ));

        // Update the content to display the skills
        updateContent();
    }

    @Override
    public SkillNodeType getSkillNodeType() {
        return SkillNodeType.GREEN;
    }
}