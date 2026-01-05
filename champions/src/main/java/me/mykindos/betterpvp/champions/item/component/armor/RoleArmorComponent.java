package me.mykindos.betterpvp.champions.item.component.armor;

import lombok.Getter;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.AbstractItemComponent;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import me.mykindos.betterpvp.core.item.component.LoreComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class RoleArmorComponent extends AbstractItemComponent implements LoreComponent {

    private final Set<Role> roles;

    private RoleArmorComponent(Set<Role> roles) {
        super("role_armor");
        this.roles = Set.copyOf(roles);
    }

    public RoleArmorComponent(Role role, Role... extra) {
        this(new HashSet<>() {{
            add(role);
            Collections.addAll(this, extra);
        }});
    }

    @Override
    public ItemComponent copy() {
        return new RoleArmorComponent(this.roles);
    }

    @Override
    public List<Component> getLines(ItemInstance item) {
        final List<Component> components = new ArrayList<>();
        components.add(Component.text("Can be worn by:", NamedTextColor.GRAY, TextDecoration.UNDERLINED));
        for (Role value : Role.values()) {
            if (!roles.contains(value)) {
                continue;
            }

            final String displayName = value.getName();
            components.add(Component.text("[", NamedTextColor.GRAY)
                    .append(Component.text("✔", NamedTextColor.GREEN))
                    .append(Component.text("] ", NamedTextColor.GRAY))
                    .append(Component.text(displayName, NamedTextColor.WHITE)));
        }
        return components;
    }

    @Override
    public int getRenderPriority() {
        return 1000;
    }
}
