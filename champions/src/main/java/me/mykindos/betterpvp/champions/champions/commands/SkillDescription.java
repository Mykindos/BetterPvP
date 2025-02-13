package me.mykindos.betterpvp.champions.champions.commands;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
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
            UtilMessage.message(player, "Skills", UtilMessage.deserialize("<green>Usage: /skilldescription <skill>"));
            return;
        }

        StringBuilder nameBuilder = new StringBuilder();
        for (String arg : args) {
            nameBuilder.append(arg).append(" ");
        }

        Optional<Skill> skillOptional = skillManager.getObject(nameBuilder.toString().trim());
        if (skillOptional.isEmpty()) {
            UtilMessage.message(player, "Skills", UtilMessage.deserialize("<yellow>%s</yellow> is not a valid skill", args[0]));
            return;
        }

        UtilMessage.message(player, "Skill", skillOptional.get().toComponent());
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
            tabCompletions.addAll(skillManager.getObjects()
                    .keySet()
                    .stream()
                    .filter(skill -> skill.toLowerCase().contains(lowercaseArg))
                    .toList());
        }
        tabCompletions.addAll(super.processTabComplete(sender, args));
        return tabCompletions;
    }
}
