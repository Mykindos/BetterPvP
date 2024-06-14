package me.mykindos.betterpvp.progression.profession.woodcutting.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillManager;
import me.mykindos.betterpvp.progression.profession.skill.builds.menu.WoodcuttingProfessionMenu;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;

@Singleton
public class WoodcuttingCommand extends Command {
    private final ProfessionProfileManager professionProfileManager;
    private final ProgressionSkillManager progressionSkillManager;

    @Inject
    public WoodcuttingCommand(ProfessionProfileManager professionProfileManager, ProgressionSkillManager progressionSkillManager) {
        this.professionProfileManager = professionProfileManager;
        this.progressionSkillManager = progressionSkillManager;
    }

    @Override
    public String getName() {
        return "woodcutting";
    }

    @Override
    public String getDescription() {
        return "Woodcutting! command";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length != 0) return;

        professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(professionProfile -> {
            new WoodcuttingProfessionMenu(professionProfile, progressionSkillManager).show(player).addCloseHandler(() -> {
                professionProfileManager.getRepository().updateBuildForGamer(player.getUniqueId(), professionProfile.getProfessionDataMap().get("Woodcutting").getBuild());
            });
        });
    }
}
