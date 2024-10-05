package me.mykindos.betterpvp.progression.profession.woodcutting.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.items.BPvPItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.profession.skill.woodcutting.TreeCompactor;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

@CustomLog
@Singleton
public class TreeCompactorCommand extends Command {
    private final TreeCompactor treeCompactor;
    private final ItemHandler itemHandler;
    private final CooldownManager cooldownManager;

    public static String TREE_COMPACTOR = "TREE_COMPACTOR";


    @Inject
    public TreeCompactorCommand(TreeCompactor treeCompactor, ItemHandler itemHandler, CooldownManager cooldownManager) {
        this.treeCompactor = treeCompactor;
        this.itemHandler = itemHandler;
        this.cooldownManager = cooldownManager;
    }

    @Override
    public @NotNull String getName() {
        return "treecompactor";
    }

    @Override
    public @NotNull String getDescription() {
        return "Compactor your logs!!!";
    }

    @Override
    public String getArgumentType(int arg) {
        if (arg == 1) {
            return ArgumentType.LOG_TYPES.name();
        }

        return ArgumentType.NONE.name();
    }

    public Component getUsage() {
        return UtilMessage.deserialize("<yellow>Usage</yellow>: <green>treecompactor <logType>");
    }

    private void feedbackMessage(@NotNull Player player, @NotNull String content) {
        final String PREFIX = "TreeCompactor";
        UtilMessage.simpleMessage(player, PREFIX, content);
    }

    /**
     * Turns a <code>logType</code> (in the form of a String) into its corresponding {@link Material}.
     * @param logType the log type to convert to a {@link Material}
     * @param ignoreAllType If this is false, then the 'All' logType will return the AIR Material, else it will be ignored
     * @return the corresponding {@link Material} converted from the logType
     */
    private @Nullable Material logTypeToMaterial(@NotNull String logType, boolean ignoreAllType) {
        boolean isThisLogTypeTheAllType = logType.equalsIgnoreCase("All");
        if (ignoreAllType && isThisLogTypeTheAllType) return null;

        String logStringMaterial = logType.toUpperCase() + "_LOG";
        return (isThisLogTypeTheAllType) ? Material.AIR : Material.getMaterial(logStringMaterial);
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (!treeCompactor.doesPlayerHaveSkill(player)) {
            feedbackMessage(player, "You do not have this command unlocked. See <green>/woodcutting");
            return;
        }

        // Only 1 argument is acceptable
        if (args.length != 1) {
            UtilMessage.message(player, "Command", getUsage());
            return;
        }

        String inputtedLogType = args[0];
        @Nullable Material inputtedLogTypeAsMaterial = logTypeToMaterial(inputtedLogType, false);


        if (inputtedLogTypeAsMaterial == null) {
            feedbackMessage(player, "Unknown log type, <white>" + inputtedLogType);
            return;
        }

        if (!cooldownManager.use(player, TREE_COMPACTOR, treeCompactor.getCooldown(), true, false)) {
            // Cooldown manager will already send a msg
            return;
        }

        List<Material> logTypesToCompact;
        if (inputtedLogTypeAsMaterial.equals(Material.AIR)) {
            logTypesToCompact = LOG_TYPES.stream()
                    .map(logType -> logTypeToMaterial(logType, true))
                    .filter(Objects::nonNull)
                    .toList();
        } else {
            logTypesToCompact = List.of(inputtedLogTypeAsMaterial);
        }

        int logsAfterCompaction = 0;

        for (Material logMaterial : logTypesToCompact) {
            while (UtilInventory.contains(player, logMaterial,  64)) {
                UtilInventory.remove(player, logMaterial, 64);

                BPvPItem item = itemHandler.getItem("progression:compacted_log");
                ItemStack itemStack = itemHandler.updateNames(item.getItemStack());

                player.getInventory().addItem(itemStack);
                logsAfterCompaction++;
            }
        }

        feedbackMessage(player, "Compacted your logs <green>" + logsAfterCompaction + "x");

        if (logsAfterCompaction == 0) cooldownManager.removeCooldown(player, TREE_COMPACTOR, true);

        log.info("{} compacted {}x logs.", player.getName(), logsAfterCompaction)
                .addClientContext(player).addLocationContext(player.getLocation()).submit();
    }
}
