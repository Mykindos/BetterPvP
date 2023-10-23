package me.mykindos.betterpvp.champions.champions.roles;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

@Data
public class Role {
    @Getter(AccessLevel.NONE)
    private final String key;

    private TextColor color;

    private Double health;

    private Material helment;
    private Material chestplate;
    private Material leggings;
    private Material boots;

    public void loadConfig(ExtendedYamlConfiguration config) {
        String path = "roles.";
        int R = config.getOrSaveInt(path + key + "color.R", 0);

    }
}
