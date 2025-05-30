package me.mykindos.betterpvp.core.client.achievements.display;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Material;

@Getter
@AllArgsConstructor
public enum Showing {
    CLIENT("Client", Material.PLAYER_HEAD),
    GAMER("Gamer", Material.IRON_SWORD);
    private final String name;
    private final Material material;
}
