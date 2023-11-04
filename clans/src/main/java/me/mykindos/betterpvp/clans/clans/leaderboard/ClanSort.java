package me.mykindos.betterpvp.clans.clans.leaderboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.stats.sort.SortType;

import java.util.function.Function;

@AllArgsConstructor
@Getter
public enum ClanSort implements SortType {

    LEVEL("Level", Clan::getLevel),
    BALANCE("Balance", Clan::getBalance),
    POINTS("Points", Clan::getPoints);

    private final String name;
    private Function<Clan, Number> function;

    public String getValue(Clan clan){
        final Number result = function.apply(clan);
        if (result instanceof Double dub) {
            return String.format("%,.2f", dub);
        } else if (result instanceof Float fl) {
            return String.format("%,.2f", fl);
        } else if (result instanceof Integer in) {
            return String.format("%,d", in);
        } else if (result instanceof Long lo) {
            return String.format("%,d", lo);
        } else {
            return result.toString();
        }
    }

}
