package me.mykindos.betterpvp.core.client.achievements;

import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.NamespacedKey;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AchievementTypes {
    @Getter
    private static final List<NamespacedKey> types = new ArrayList<>();
    public static final NamespacedKey DEATH_TYPE = createType("core", "death");

    private static NamespacedKey createType(String namespace, String key){
        final NamespacedKey namespacedKey = new NamespacedKey(namespace, key);
        types.add(namespacedKey);
        return namespacedKey;
    }
}
