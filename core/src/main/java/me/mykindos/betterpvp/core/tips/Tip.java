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

    public Tip(int categoryWeight, int weight, Component component) {
        this.categoryWeight = categoryWeight;
        this.weight = weight;
        this.component = component;
    }

    public Tip(int categoryWeight, int weight) {
        this(categoryWeight, weight, Component.empty());
    }



    public abstract String getName();

    public boolean isValid(Player player) {
        return false;
    }

    public Component generateComponent() {
        return Component.empty();
    }

}
