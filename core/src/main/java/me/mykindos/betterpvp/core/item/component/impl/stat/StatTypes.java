package me.mykindos.betterpvp.core.item.component.impl.stat;

import lombok.experimental.UtilityClass;
import me.mykindos.betterpvp.core.combat.cause.DamageCause;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Registry of all built-in stat types.
 * Custom stat types can be registered via StatTypeRegistry.
 */
@UtilityClass
public final class StatTypes {

    // Constants for built-in stat types
    public static final StatType<Double> HEALTH = StatType.builder(new NamespacedKey("betterpvp", "health"), Double.class)
            .name("Health")
            .description("Increases the health of the wearer.")
            .displayColor(TextColor.color(255, 0, 0))
            .build();

    public static final StatType<Double> MELEE_DAMAGE = StatType.builder(new NamespacedKey("betterpvp", "melee-damage"), Double.class)
            .name("Damage")
            .displayColor(TextColor.color(255, 179, 28))
            .description("Increases the melee damage dealt by the item.")
            .valuePredicate(value -> value >= 0)
            .build();

    public static final StatType<Double> MELEE_ATTACK_SPEED = StatType.builder(new NamespacedKey("betterpvp", "melee-attack-speed"), Double.class)
            .name("Melee Attack Speed")
            .displayColor(TextColor.color(229, 235, 52))
            .shortName("Attacks/second")
            .description("Increases the melee attack speed of the holder.")
            .percentage(true)
            .stringValueProvider(value -> {
                double hitsPerSecond = 1000L / (DamageCause.DEFAULT_DELAY / (1 + value));
                return "+" + UtilFormat.formatNumber(hitsPerSecond, 2, false);
            })
            .valuePredicate(Objects::nonNull)
            .build();

    public static final StatType<Double> MOVEMENT = StatType.builder(new NamespacedKey("betterpvp", "move-speed"), Double.class)
            .name("Movement")
            .displayColor(TextColor.color(66, 221, 245))
            .description("Increases the walking speed of the holder.")
            .percentage(true)
            .valuePredicate(value -> value > 0)
            .lifecycleHooks(new StatType.ItemStatLifecycleHooks<>() {
                @Override
                public void onApply(Item item, ItemStack stack, Double value) {
                    @NotNull AttributeModifier modifier = new AttributeModifier(
                            new NamespacedKey("betterpvp", "move-speed"),
                            value,
                            AttributeModifier.Operation.ADD_NUMBER
                    );
                    stack.editMeta(meta -> meta.addAttributeModifier(Attribute.MOVEMENT_SPEED, modifier));
                }

                @Override
                public void onRemove(Item item, ItemStack stack, Double value) {
                    @NotNull AttributeModifier modifier = new AttributeModifier(
                            new NamespacedKey("betterpvp", "move-speed"),
                            value,
                            AttributeModifier.Operation.ADD_NUMBER
                    );
                    stack.editMeta(meta -> meta.removeAttributeModifier(Attribute.MOVEMENT_SPEED, modifier));
                }
            })
            .build();

    /**
     * Called by dependency injection to register all built-in types.
     * Must be called after StatTypeRegistry is created.
     *
     * @param registry The registry to register types to
     */
    public static void registerAll(StatTypeRegistry registry) {
        registry.register(HEALTH);
        registry.register(MELEE_DAMAGE);
        registry.register(MELEE_ATTACK_SPEED);
        registry.register(MOVEMENT);
    }
}
