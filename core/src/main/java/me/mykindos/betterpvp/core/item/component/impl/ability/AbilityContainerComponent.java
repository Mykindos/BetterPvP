package me.mykindos.betterpvp.core.item.component.impl.ability;

import com.google.common.base.Preconditions;
import lombok.Builder;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import me.mykindos.betterpvp.core.item.component.LoreComponent;
import me.mykindos.betterpvp.core.item.component.impl.ContainerComponent;
import me.mykindos.betterpvp.core.utilities.ComponentWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents a component that can hold abilities for an item.
 * <p>
 * This container cannot contain conflicting abilities with
 * the same trigger type.
 */
@Builder
public class AbilityContainerComponent extends ContainerComponent<ItemAbility> implements LoreComponent {

    public AbilityContainerComponent(@NotNull List<@NotNull ItemAbility> abilities) {
        super("abilities");
        this.container = abilities;

        Preconditions.checkArgument(!abilities.isEmpty(), "Abilities cannot be empty");
        final long nonPassiveCount = abilities.stream()
                .filter(a -> !a.getTriggerType().equals(TriggerTypes.PASSIVE))
                .count();
        final long nonPassiveDistinct = abilities.stream()
                .map(ItemAbility::getTriggerType)
                .filter(triggerType -> !triggerType.equals(TriggerTypes.PASSIVE))
                .distinct()
                .count();
        Preconditions.checkArgument(nonPassiveDistinct == nonPassiveCount, "Non-passive abilities cannot have the same trigger type");
    }

    public @NotNull Optional<ItemAbility> getAbility(@NotNull TriggerTypes triggerType) {
        return container.stream()
                .filter(ability -> ability.getTriggerType() == triggerType)
                .findFirst();
    }

    @Override
    public ItemComponent copy() {
        return new AbilityContainerComponent(container);
    }

    @Override
    public List<Component> getLines(ItemInstance item) {
        final List<Component> lines = new ArrayList<>();

        for (int i = 0; i < container.size(); i++) {
            final ItemAbility ability = container.get(i);
            final Component text = Component.text(ability.getDescription(), NamedTextColor.WHITE);
            final List<Component> components = ComponentWrapper.wrapLine(text, 30, true);

            final TextComponent title = Component.text(ability.getName(), NamedTextColor.YELLOW)
                    .appendSpace()
                    .append(ability.getTriggerType().getName().applyFallbackStyle(Style.style(NamedTextColor.YELLOW, TextDecoration.BOLD)));
            components.addFirst(title);
            if (i < container.size() - 1) {
                components.add(Component.empty()); // Add a separator between abilities
            }

            lines.addAll(components);
        }

        return lines;
    }

    @Override
    public int getRenderPriority() {
        return -1;
    }

    public static class AbilityContainerComponentBuilder {
        private final List<ItemAbility> container = new ArrayList<>();

        /**
         * Add a single ability to the container.
         * @param ability The ability to add
         * @return this builder
         */
        public AbilityContainerComponentBuilder ability(ItemAbility ability) {
            this.container.add(ability);
            return this;
        }

        /**
         * Add multiple abilities to the container.
         * @param abilities The abilities to add
         * @return this builder
         */
        public AbilityContainerComponentBuilder abilities(ItemAbility... abilities) {
            this.container.addAll(List.of(abilities));
            return this;
        }

        /**
         * Add multiple abilities to the container.
         * @param abilities The abilities to add
         * @return this builder
         */
        public AbilityContainerComponentBuilder abilities(List<ItemAbility> abilities) {
            this.container.addAll(abilities);
            return this;
        }

        public AbilityContainerComponent build() {
            return new AbilityContainerComponent(container);
        }
    }

}
