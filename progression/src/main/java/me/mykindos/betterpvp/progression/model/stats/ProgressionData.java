package me.mykindos.betterpvp.progression.model.stats;

import lombok.Getter;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.event.PlayerProgressionExperienceEvent;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.UUID;

/**
 * Represents a player's progression data within a single {@link ProgressionTree}
 * @param <T> The type of {@link ProgressionTree} this data is for.
 */
@Getter
public abstract class ProgressionData<T extends ProgressionTree> {

    /**
     * Their total experience in this tree.
     */
    private @Range(from = 0, to = Long.MAX_VALUE) long experience;
    private int level = 0;
    private T tree;

    public final void grantExperience(@Range(from = 0, to = Integer.MAX_VALUE) long amount, @Nullable Player player) {
        final int previous = level;
        this.experience += amount;
        this.level = getLevelFromExperience((int) experience);
        if (player != null) {
            final PlayerProgressionExperienceEvent event = new PlayerProgressionExperienceEvent(tree, player, amount, this, level, previous);
            UtilServer.runTask(JavaPlugin.getPlugin(Progression.class), () -> UtilServer.callEvent(event));
        }
    }

    public final void grantExperience(@Range(from = 0, to = Integer.MAX_VALUE) long amount) {
        grantExperience(amount, null);
    }

    public void setExperience(long experience) {
        this.experience = experience;
        this.level = getLevelFromExperience((int) experience);
    }

    private int getLevelFromExperience(int experience) {
        return (int) (Math.sqrt(experience) * Math.log10(experience) / 22) + 1;
    }

    public int getLevel() {
        return level;
    }

    void setTree(T tree) {
        this.tree = tree;
    }

    protected abstract void prepareUpdates(@NotNull UUID uuid, @NotNull Database database, String databasePrefix);

    public abstract Component[] getDescription();
}
