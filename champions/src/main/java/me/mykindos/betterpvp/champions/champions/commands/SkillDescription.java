package me.mykindos.betterpvp.champions.champions.commands;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Singleton
public class SkillDescription extends Command {
    private final ChampionsSkillManager skillManager;

    @Inject
    public SkillDescription(ChampionsSkillManager skillManager) {
        this.skillManager = skillManager;
    }

    @Override
    public String getName() {
        return "skilldescription";
    }

    @Override
    public String getDescription() {
        return "Prints the description of the skill to your chat";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            UtilMessage.message(player, "Skills", UtilMessage.deserialize("<green>Usage: /skilldescription <skill> [level]"));
            return;
        }

        Optional<Skill> skillOptional = skillManager.getObject(args[0].replace("_", " "));
        if (skillOptional.isEmpty()) {
            UtilMessage.message(player, "Skills", UtilMessage.deserialize("<yellow>%s</yellow> is not a valid skill", args[0]));
            return;
        }

        Skill skill = skillOptional.get();

        int level = 1;
        if (args.length > 1) {
            try {
                level = Integer.parseInt(args[1]);
                if (level > skill.getMaxLevel() + 1 || level < 0) {
                    throw new NumberFormatException("level too high");
                }
            } catch (NumberFormatException ex) {
                UtilMessage.message(player, "Skill", UtilMessage.deserialize("<green>%s</green> is not a valid number. Must be a number between <green>1</green> and <green>%s</green>", args[0], skill.getMaxLevel() + 1));
                return;
            }
        }

        Component component = UtilMessage.deserialize("<yellow>%s</yellow> (<green>%s</green>)", skill.getName(), level);
        for (Component line : skill.parseDescription(level)) {
            component = component.appendNewline().append(line);
        }

        UtilMessage.message(player, "Skill", component);

    }

    @Override
    public String getArgumentType(int argCount) {
        return (argCount == 1 ? "SKILLS" : ArgumentType.NONE.name());
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        //explicitly add null if it is a player
        List<String> tabCompletions = new ArrayList<>();

        if (args.length == 0) return super.processTabComplete(sender, args);

        String lowercaseArg = args[args.length - 1].toLowerCase();
        if (getArgumentType(args.length).equals("SKILLS")) {
            tabCompletions.addAll(skillManager.getObjects().keySet().stream()
                    .map(string -> string.replace(" ", "_"))
                    .filter(skill -> skill.toLowerCase().contains(lowercaseArg)).toList());
        }
        tabCompletions.addAll(super.processTabComplete(sender, args));
        return tabCompletions;
    }
}
