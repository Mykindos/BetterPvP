package me.mykindos.betterpvp.clans.clans.tips;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.clans.clans.Clan;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.function.Predicate;

@Getter
public abstract class Tip {

    private final int categoryWeight;

    private final int weight;

    @Setter
    private Component component;

    protected Tip(int categoryWeight, int weight, Component component) {
        this.categoryWeight = categoryWeight;
        this.weight = weight;
        this.component = component;
    }

    protected Tip(int categoryWeight, int weight) {
        this.categoryWeight = categoryWeight;
        this.weight = weight;
    }

    public boolean isValid(Player player, Clan clan) {
        return true;
    }


    private Component generateComponent() {
        return Component.empty();
    }
}
