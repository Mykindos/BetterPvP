package me.mykindos.betterpvp.progression.profession.fishing.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillManager;
import me.mykindos.betterpvp.progression.profession.skill.builds.menu.FishingProfessionMenu;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;

@Singleton
public class FishingCommand extends Command {

    private final ProfessionProfileManager profileManager;
    private final ProgressionSkillManager progressionSkillManager;

    @Inject
    public FishingCommand(ProfessionProfileManager profileManager, ProgressionSkillManager progressionSkillManager) {

        this.profileManager = profileManager;
        this.progressionSkillManager = progressionSkillManager;
    }

    @Override
    public String getName() {
        return "fishing";
    }

    @Override
    public String getDescription() {
        return "Fishing command";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length != 0) return;

        profileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            new FishingProfessionMenu(profile, progressionSkillManager).show(player).addCloseHandler(() -> {
                profileManager.getRepository().updateBuildForGamer(player.getUniqueId(), profile.getProfessionDataMap().get("Fishing").getBuild());
            });
        });

    }
}
