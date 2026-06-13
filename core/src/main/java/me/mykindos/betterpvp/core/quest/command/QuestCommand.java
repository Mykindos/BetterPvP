package me.mykindos.betterpvp.core.quest.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.quest.QuestInstance;
import me.mykindos.betterpvp.core.quest.QuestManager;
import me.mykindos.betterpvp.core.quest.QuestRegistry;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class QuestCommand extends Command {

    private final QuestManager questManager;
    private final QuestRegistry questRegistry;

    @Inject
    private QuestCommand(QuestManager questManager, QuestRegistry questRegistry) {
        this.questManager = questManager;
        this.questRegistry = questRegistry;
    }

    @Override
    public String getName() {
        return "quest";
    }

    @Override
    public String getDescription() {
        return "Start and track quests";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 0) {
            UtilMessage.simpleMessage(player, "Quest", "Usage: /quest <start|list|debug> [id]");
            return;
        }
        switch (args[0].toLowerCase()) {
            case "debug" -> debug(player);
            case "start" -> {
                if (args.length < 2) {
                    UtilMessage.simpleMessage(player, "Quest", "Usage: /quest start <id>");
                    return;
                }
                questManager.startQuest(player, args[1]);
            }
            case "list" -> {
                List<QuestInstance> active = questManager.activeFor(player);
                if (active.isEmpty()) {
                    UtilMessage.simpleMessage(player, "Quest", "You have no active quests.");
                    return;
                }
                for (QuestInstance instance : active) {
                    String name = questRegistry.get(instance.getQuestId())
                            .map(def -> def.getName()).orElse(instance.getQuestId());
                    UtilMessage.message(player, "Quest", "<green>" + name + " <gray>(" + instance.getCurrentStages().size() + " stage(s) active)");
                    instance.getProgress().forEach((key, value) ->
                            UtilMessage.message(player, "Quest", "  <gray>" + key + ": " + value + "/" + instance.getTargets().getOrDefault(key, 1)));
                }
            }
            default -> UtilMessage.simpleMessage(player, "Quest", "Usage: /quest <start|list|debug> [id]");
        }
    }

    /** Dump the loaded quest catalogue and this player's live instances/progress. */
    private void debug(Player player) {
        var loaded = questRegistry.getLoaded();
        UtilMessage.message(player, "Quest", "<gray>Loaded quests: <white>" + loaded.size());
        loaded.forEach((id, def) -> UtilMessage.message(player, "Quest",
                "  <yellow>" + id + " <gray>— " + def.getName() + " (" + def.getNodes().size() + " stage(s))"));

        List<QuestInstance> active = questManager.activeFor(player);
        UtilMessage.message(player, "Quest", "<gray>Your active instances: <white>" + active.size());
        for (QuestInstance instance : active) {
            UtilMessage.message(player, "Quest", "  <green>" + instance.getQuestId()
                    + " <gray>status=" + instance.getStatus() + " stages=" + instance.getCurrentStages());
            instance.getProgress().forEach((key, value) -> UtilMessage.message(player, "Quest",
                    "    <gray>" + key + ": " + value + "/" + instance.getTargets().getOrDefault(key, 1)));
        }
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("start", "list", "debug");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return new ArrayList<>(questRegistry.getLoaded().keySet().stream()
                    .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase())).toList());
        }
        return super.processTabComplete(sender, args);
    }
}
