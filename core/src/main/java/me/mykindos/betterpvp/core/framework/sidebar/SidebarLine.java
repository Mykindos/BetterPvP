package me.mykindos.betterpvp.core.framework.sidebar;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
import me.mykindos.betterpvp.core.framework.sidebar.protocol.ChannelInjector;
import me.mykindos.betterpvp.core.framework.sidebar.protocol.ScoreboardPackets;
import me.mykindos.betterpvp.core.framework.sidebar.text.TextProvider;
import me.mykindos.betterpvp.core.framework.sidebar.util.lang.ThrowingFunction;
import me.mykindos.betterpvp.core.framework.sidebar.util.lang.ThrowingPredicate;
import me.mykindos.betterpvp.core.framework.sidebar.util.lang.ThrowingSupplier;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

@Getter
@ToString
public class SidebarLine {

    private final String teamName;

    @Setter(AccessLevel.PACKAGE)
    private int score = -1;

    private final int index;
    private final boolean staticText;

    // for internal use
    BukkitTask updateTask;

    private ThrowingFunction<Player, TextComponent, Throwable> updater;
    private ThrowingPredicate<Player, Throwable> displayCondition;
    private final TextProvider<TextComponent> textProvider;

    SidebarLine(@NonNull ThrowingFunction<Player, TextComponent, Throwable> updater,
                @NonNull String teamName,
                boolean staticText,
                int index,
                @NonNull TextProvider<TextComponent> textProvider,
                @NonNull ThrowingPredicate<Player, Throwable> displayCondition) {
        this.updater = updater;
        this.teamName = teamName;
        this.staticText = staticText;
        this.index = index;
        this.displayCondition = displayCondition;
        this.textProvider = textProvider;
    }

    public BukkitTask updatePeriodically(long delay, long period, @NonNull Sidebar sidebar) {
        Preconditions.checkState(!isStaticText(), "Cannot set updater for static text line");

        if (updateTask != null) {
            Preconditions.checkState(updateTask.isCancelled(),
                    "Update task for line %s is already running. Cancel it first.", this);
            sidebar.taskIds.remove(updateTask.getTaskId());
        }

        BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(sidebar.getPlugin(),
                () -> sidebar.updateLine(this), delay, period);

        this.updateTask = task;

        sidebar.bindBukkitTask(task);

        return task;
    }

    /**
     * Sets visibility predicate for this line. Visibility predicate is a function that takes player
     * as an argument and returns boolean value. If predicate returns true, line will be visible for
     * this player, otherwise - invisible.
     *
     * @param displayCondition - visibility predicate
     */
    public void setDisplayCondition(@NonNull ThrowingPredicate<Player, Throwable> displayCondition) {
        this.displayCondition = displayCondition;
    }

    /**
     * Sets updater for this line. Updater is a function that takes player as an argument and returns
     * text that will be displayed for this player.
     *
     * @param updater - updater function
     */
    public void setUpdater(@NonNull ThrowingFunction<Player, TextComponent, Throwable> updater) {
        Preconditions.checkState(!isStaticText(), "Cannot set updater for static text line");
        this.updater = updater;
    }

    /**
     * Sets updater for this line without player parameter
     *
     * @param updater - updater function
     */
    public void setUpdater(@NonNull ThrowingSupplier<TextComponent, Throwable> updater) {
        Preconditions.checkState(!isStaticText(), "Cannot set updater for static text line");
        this.updater = player -> updater.get();
    }

    void updateTeam(@NonNull Player player, @NonNull String objective) throws Throwable {
        boolean visible = displayCondition.test(player);

        if (!isStaticText() && visible) {
            TextComponent text = updater.apply(player);
            sendPacket(player, ScoreboardPackets.createTeamPacket(ScoreboardPackets.TEAM_UPDATED, index, teamName,
                    player, text, textProvider));
        }

        if (!visible) {
            // if player doesn't meet display condition, remove score
            sendPacket(player, ScoreboardPackets.createScorePacket(player, 1, objective, score, index));
            return;
        }

        sendPacket(player, ScoreboardPackets.createScorePacket(player, 0, objective, score, index));
    }

    void removeTeam(@NonNull Player player, @NonNull String objective) {
        sendPacket(player, ScoreboardPackets.createScorePacket(player, 1, objective, score, index));

        sendPacket(player, ScoreboardPackets.createTeamPacket(ScoreboardPackets.TEAM_REMOVED, index, teamName,
                player, null, textProvider));
    }

    void createTeam(@NonNull Player player, @NonNull String objective) throws Throwable {
        boolean visible = displayCondition.test(player);

        TextComponent text = visible ? updater.apply(player) : textProvider.emptyMessage();

        sendPacket(player, ScoreboardPackets.createTeamPacket(ScoreboardPackets.TEAM_CREATED, index, teamName,
                player, text, textProvider));

        if (visible) {
            sendPacket(player, ScoreboardPackets.createScorePacket(player, 0, objective, score, index));
        }
    }

    @SneakyThrows
    static void sendPacket(@NonNull Player player, @NonNull ByteBuf packet) {
        ChannelInjector.IMP.sendPacket(player, packet);
    }
}
