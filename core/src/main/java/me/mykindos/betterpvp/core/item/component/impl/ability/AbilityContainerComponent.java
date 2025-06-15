package me.mykindos.betterpvp.core.item.component.impl.ability;

import com.google.common.base.Preconditions;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.AbstractItemComponent;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import me.mykindos.betterpvp.core.item.component.LoreComponent;
import me.mykindos.betterpvp.core.utilities.ComponentWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
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
public class AbilityContainerComponent extends AbstractItemComponent implements LoreComponent {

    @Singular
    @Getter
    private @NotNull List<@NotNull ItemAbility> abilities;

    public AbilityContainerComponent(@NotNull List<@NotNull ItemAbility> abilities) {
        super("abilities");
        this.abilities = abilities;

        Preconditions.checkArgument(!abilities.isEmpty(), "Abilities cannot be empty");
        Preconditions.checkArgument(abilities.stream()
                .map(ItemAbility::getTriggerType)
                .distinct()
                .count() == abilities.size(),
                "Abilities cannot have the same trigger type");
    }

    public @NotNull Optional<ItemAbility> getAbility(@NotNull TriggerType triggerType) {
        return abilities.stream()
                .filter(ability -> ability.getTriggerType() == triggerType)
                .findFirst();
    }

    @Override
    public ItemComponent copy() {
        return AbilityContainerComponent.builder()
                .abilities(abilities)
                .build();
    }

    @Override
    public List<Component> getLines(ItemInstance item) {
        final List<Component> lines = new ArrayList<>();

        // todo: add energy and cooldown icons
        for (int i = 0; i < abilities.size(); i++) {
            final ItemAbility ability = abilities.get(i);
            final Component text = Component.text(ability.getDescription(), NamedTextColor.WHITE);
            final List<Component> components = ComponentWrapper.wrapLine(text, 30, true);

            String triggerType = ability.getTriggerType().getName().toUpperCase();
            final TextComponent title = Component.text(ability.getName(), NamedTextColor.YELLOW)
                    .appendSpace()
                    .append(Component.text(triggerType, NamedTextColor.YELLOW, TextDecoration.BOLD));
            components.addFirst(title);
            if (i < abilities.size() - 1) {
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        AbilityContainerComponent that = (AbilityContainerComponent) o;
        return abilities.equals(that.abilities);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        // the abilities list hashcode is not the same for lists with the same elements, despite being equal
        // so we sum the hashcodes of each ability
        result = 31 * result + abilities.stream().mapToInt(ItemAbility::hashCode).sum();
        return result;
    }
}
