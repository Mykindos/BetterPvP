package me.mykindos.betterpvp.core.components.champions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;

@AllArgsConstructor
@Getter
public enum Role {

    ASSASSIN("Assassin", 36, TextColor.color(224, 112, 0),
            Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
            80, 100, 20, true),
    KNIGHT("Knight", 50, TextColor.color(227, 227, 227),
            Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
            60, 40, 60, false),
    BRUTE("Brute", 50, TextColor.color(112, 255, 241),
            Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
            80, 60, 40, false),
    RANGER("Ranger", 36, TextColor.color(148, 148, 148),
            Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
            100, 80, 20, true),
    MAGE("Mage", 40, TextColor.color(255, 237, 69),
            Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
            60, 20, 100, false),
    WARLOCK("Warlock", 40, TextColor.color(117, 117, 117),
            Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
            60, 20, 100, false);

    public static final Role DEFAULT = KNIGHT;

    private final String name;
    private final double health;
    private final TextColor color;
    private final Material helmet;
    private final Material chestplate;
    private final Material leggings;
    private final Material boots;
    /**
     * How hard this class hits, as a percentage of the strongest a class can be. These three ratings are a
     * hand-tuned comparison between classes for the class selector, not a readout of any item or attribute.
     */
    private final int damageRating;
    /**
     * How mobile this class is, on the same 0-100 comparison scale as {@link #damageRating}.
     */
    private final int mobilityRating;
    /**
     * How much this class does for the players around it - healing, shielding and buffing - on the same
     * 0-100 comparison scale as {@link #damageRating}.
     */
    private final int supportRating;
    /**
     * Whether this class is handed a bow and arrows when it equips.
     */
    private final boolean usesBow;

    public String getPrefix() {
        return name.substring(0, 1);
    }

    public String getName() {
        return name;
    }

    /**
     * The player-facing, translatable display name of this role (e.g. {@code core.role.assassin.name}).
     * {@link #getName()} remains the stable internal identifier (DB, config, registry, map keys) and must
     * not be used for display.
     *
     * @return the translatable display-name component
     */
    public Component getDisplayName() {
        return Translations.component("core.role." + name().toLowerCase() + ".name");
    }

    /**
     * The player-facing, translatable description of this role (e.g. {@code core.role.assassin.description}).
     *
     * @return the translatable description component
     */
    public Component getDescriptionComponent() {
        return Translations.component("core.role." + name().toLowerCase() + ".description");
    }

    /**
     * A couple of lines on what playing this class feels like, for the class selector. The longer
     * {@link #getDescriptionComponent()} is too much to sit under a helmet in a menu.
     *
     * @return the translatable summary component
     */
    public Component getSummaryComponent() {
        return Translations.component("core.role." + name().toLowerCase() + ".summary");
    }

    public Material getMaterial(EquipmentSlot equipmentSlot) {
        return switch (equipmentSlot) {
            case HEAD -> helmet;
            case CHEST -> chestplate;
            case LEGS -> leggings;
            case FEET -> boots;
            default -> throw new IllegalArgumentException("No material for equipment slot: " + equipmentSlot);
        };
    }

}