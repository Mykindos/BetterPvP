package me.mykindos.betterpvp.champions.champions.roles;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.data.SoundProvider;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Hit sound provider for {@link Role}
 */
@Singleton
public class RoleSoundProvider implements SoundProvider {

    @Inject
    private RoleManager roleManager;

    @Override
    public @Nullable Sound apply(@NotNull CustomDamageEvent event) {
        if (!(event.getDamagee() instanceof Player player)) {
            return SoundProvider.DEFAULT.apply(event);
        }

        final Optional<Role> roleOpt = roleManager.getObject(player.getUniqueId());
        if (roleOpt.isEmpty()) {
            return SoundProvider.DEFAULT.apply(event);
        }

        final Role role = roleOpt.get();
        final net.kyori.adventure.sound.Sound.Builder sound = net.kyori.adventure.sound.Sound.sound();
        sound.source(SoundProvider.getSource(event.getDamagee()));
        sound.volume(1f);

        switch (role) {
            case KNIGHT -> sound.type(org.bukkit.Sound.ENTITY_BLAZE_HURT.key()).pitch(0.7f);
            case ASSASSIN -> sound.type(org.bukkit.Sound.ENTITY_ARROW_SHOOT.key()).pitch(2f);
            case BRUTE -> sound.type(org.bukkit.Sound.ENTITY_BLAZE_HURT.key()).pitch(0.9f);
            case RANGER -> sound.type(org.bukkit.Sound.ENTITY_ITEM_BREAK.key()).pitch(1.4f);
            case MAGE -> sound.type(org.bukkit.Sound.ENTITY_ITEM_BREAK.key()).pitch(1.8f);
            case WARLOCK -> sound.type(org.bukkit.Sound.ENTITY_BLAZE_HURT.key()).pitch(0.6f);
        }

        return sound.build();
    }
}
