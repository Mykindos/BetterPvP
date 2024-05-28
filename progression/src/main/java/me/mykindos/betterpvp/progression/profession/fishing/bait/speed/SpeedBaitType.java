package me.mykindos.betterpvp.progression.profession.fishing.bait.speed;

import lombok.Getter;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.profession.fishing.bait.SimpleBaitType;
import me.mykindos.betterpvp.progression.profession.fishing.model.Bait;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

@Getter
public class SpeedBaitType extends SimpleBaitType {

    private double multiplier;

    public SpeedBaitType(String key) {
        super(key);
    }

    @Override
    public void loadConfig(@NotNull ExtendedYamlConfiguration config) {
        super.loadConfig(config);
        this.multiplier = config.getOrSaveObject("fishing.bait." + key + ".multiplier", 1.0D, Double.class);
    }

    @Override
    public @NotNull Bait generateBait() {
        return new SpeedBait(this);
    }

    @Override
    public Component[] getDescription() {
        return new Component[] {
                UtilMessage.deserialize("<gray>Radius: <yellow>%s blocks", getRadius()).decoration(TextDecoration.ITALIC, false),
                UtilMessage.deserialize("<gray>Speed: <yellow>%sx", multiplier).decoration(TextDecoration.ITALIC, false)
        };
    }
}
