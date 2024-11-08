package me.mykindos.betterpvp.progression.profession.mining.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillManager;
import me.mykindos.betterpvp.progression.profession.skill.mining.CaveCallerSkill;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@CustomLog
@Singleton
public class CaveCallerCommand extends Command {
    private final ProfessionProfileManager profileManager;
    private final ProgressionSkillManager progressionSkillManager;
    private final CaveCallerSkill caveCallerSkill;

    @Inject
    public CaveCallerCommand(ProfessionProfileManager profileManager, ProgressionSkillManager progressionSkillManager,
                             CaveCallerSkill caveCallerSkill) {

        this.profileManager = profileManager;
        this.progressionSkillManager = progressionSkillManager;
        this.caveCallerSkill = caveCallerSkill;
    }

    @Override
    public @NotNull String getName() {
        return "cavecaller";
    }

    @Override
    public @NotNull String getDescription() {
        return "Toggle Cave Caller on/off";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (!caveCallerSkill.doesPlayerHaveSkill(player)) return;

        UUID playerUUID = player.getUniqueId();
        Component result;

        if (caveCallerSkill.playersWithPerkActivated.contains(playerUUID)) {
            caveCallerSkill.playersWithPerkActivated.remove(playerUUID);
            result = Component.text("disabled", NamedTextColor.RED);
        } else {
            caveCallerSkill.playersWithPerkActivated.add(playerUUID);
            result = Component.text("enabled", NamedTextColor.GREEN);
        }

        UtilMessage.simpleMessage(player, "Command", Component.text("Cave Caller: ").append(result));
    }
}
