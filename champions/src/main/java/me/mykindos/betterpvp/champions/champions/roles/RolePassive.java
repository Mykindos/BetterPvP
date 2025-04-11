package me.mykindos.betterpvp.champions.champions.roles;

import lombok.Data;
import net.kyori.adventure.text.Component;

@Data
public class RolePassive {
    private final String name;
    private final Component description;
    private final boolean isBuff;
}
