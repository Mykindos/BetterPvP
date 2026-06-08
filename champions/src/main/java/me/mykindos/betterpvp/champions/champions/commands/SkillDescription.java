package me.mykindos.betterpvp.champions.champions.commands;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
        return "champions.command.skill-description.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            UtilMessage.message(player, "core.prefix.skills", Translations.component("champions.command.skilldescription.usage").color(NamedTextColor.GREEN));
            return;
        }

        Optional<Skill> skillOptional = skillManager.getObject(args[0].replace("_", " "));
        if (skillOptional.isEmpty()) {
            UtilMessage.message(player, "core.prefix.skills", "champions.command.skilldescription.invalid-skill", Component.text(args[0], NamedTextColor.YELLOW));
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
                UtilMessage.message(player, "core.prefix.skill", "champions.command.skilldescription.invalid-number", Component.text(args[0], NamedTextColor.GREEN), Component.text("1", NamedTextColor.GREEN), Component.text(String.valueOf(skill.getMaxLevel() + 1), NamedTextColor.GREEN));
                return;
            }
        }

        Component component = Component.empty()
                .append(skill.getDisplayName().color(NamedTextColor.YELLOW))
                .append(Component.text(" ("))
                .append(Component.text(level, NamedTextColor.GREEN))
                .append(Component.text(")"));
        for (Component line : skill.getDescription(level)) {
            Component styled = line.colorIfAbsent(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false);
            component = component.appendNewline().append(styled);
        }

        UtilMessage.message(player, "core.prefix.skill", component);

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
