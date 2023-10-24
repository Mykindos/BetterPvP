package me.mykindos.betterpvp.core.components.champions;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.Sound;

@Data
public class Role {
    @Getter(AccessLevel.NONE)
    private final String key;

    private String prefix;

    private TextColor color;

    private Double maxHealth;
    private Double arrowDamage;

    private boolean dealKnockback;
    private boolean takeKnockback;

    private Sound damageSound;
    private float damageVolume;
    private float damagePitch;

    private Material[] armor = new Material[4];

    public void loadConfig(ExtendedYamlConfiguration config) {
        String path = "class." + key;
        prefix = config.getOrSaveString(path + "prefix", key.substring(0, 1));

        int R = config.getOrSaveInt(path + "color.R", 0);
        int G = config.getOrSaveInt(path + "color.G", 0);
        int B = config.getOrSaveInt(path+ "color.B", 0);

        color = TextColor.color(R, G, B);

        maxHealth = config.getOrSaveObject(path + "maxHealth", 20.0, Double.class);
        arrowDamage = config.getOrSaveObject(path + "arrowDamage", 0.0, Double.class);

        dealKnockback = config.getOrSaveBoolean(path + "dealKnockback", true);
        takeKnockback = config.getOrSaveBoolean(path + "takeKnockback", true);

        String soundKey = config.getOrSaveString(path + "sound.damageSound", "ENTITY_BLAZE_HURT");
        if (soundKey == null) {
            throw new IllegalArgumentException("Sound key cannot be null!");
        }
        try {
            this.damageSound = Sound.valueOf(soundKey.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid sound key: " + soundKey, e);
        }

        damageVolume = config.getOrSaveObject(path + "sound.volume", 1.0F, Float.class);
        damagePitch = config.getOrSaveObject(path + "sound.pitch", 0.7F, Float.class);

        String[] armorTypes = {"helmet", "chestplate", "leggings", "boots"};
        for (int i = 0; i < armorTypes.length; i++) {
            final String type = armorTypes[i];
            final String materialKey = config.getOrSaveString(path, "LEATHER_" + type.toUpperCase());
            if (materialKey == null) {
                throw new IllegalArgumentException(type.toUpperCase() + " material key cannot be null!");
            }
            try {
                this.armor[i] = Material.valueOf(materialKey.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid material key: " + materialKey, e);
            }
        }

    }

    public String getName() {
        return key;
    }

    public Material getHelmet() {
        return armor[0];
    }

    public Material getChestplate() {
        return armor[1];
    }

    public Material getLeggings() {
        return armor[2];
    }

    public Material getBoots() {
        return armor[3];
    }
}
