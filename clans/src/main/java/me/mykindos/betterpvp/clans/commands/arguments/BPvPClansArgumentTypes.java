package me.mykindos.betterpvp.clans.commands.arguments;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentTypes;

public class BPvPClansArgumentTypes {
    private static ClanArgument CLAN;
    private static ExecutorEnemyClanArgument EXECUTORENEMYCLAN;
    @Inject
    public BPvPClansArgumentTypes(Clans plugin) {
        BPvPClansArgumentTypes.CLAN = (ClanArgument) BPvPArgumentTypes.createArgumentType(plugin, ClanArgument.class);
        BPvPClansArgumentTypes.EXECUTORENEMYCLAN = (ExecutorEnemyClanArgument) BPvPArgumentTypes.createArgumentType(plugin, ExecutorEnemyClanArgument.class);
    }

    /**
     * Prompts the sender with any {@link Clan}. Guarantees a valid return {@link Clan}.
     * @return the {@link ClanArgument}
     */
    public static ClanArgument clan() {
        return CLAN;
    }

    /**
     * Prompts the sender with the executor's {@link Clan}'s enemies. Guarantees a valid return {@link Clan}, but not if it is an enemy of the executor.
     * @return the {@link ExecutorEnemyClanArgument}
     */
    public static ExecutorEnemyClanArgument executorEnemyClan() {
        return EXECUTORENEMYCLAN;
    }
}
