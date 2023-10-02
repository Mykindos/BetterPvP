package me.mykindos.betterpvp.core.tips;

import com.google.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Singleton
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
        this.component = generateComponent();
    }

    public abstract String getName();

    public boolean isValid(Player player) {
        return false;
    }


    private Component generateComponent() {
        return Component.empty();
    }
}
