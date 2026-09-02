package me.mykindos.betterpvp.champions.combat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * How every bow in the game behaves, in one place: the damage a fully drawn arrow lands for, and whether the
 * bow will fire at all without a bow skill prepared. The damage pipeline and the class selector both read
 * these, so a number the menu promises is the number the arrow deals.
 * <p>
 * A class with no {@code arrowDamage} of its own falls back to {@link #getBaseArrowDamage()}; only a class
 * that {@link Role#isUsesBow() carries a bow} is configurable, since nothing else can loose an arrow.
 */
@Singleton
public class RoleBowService implements ConfigAccessor {

    private double baseArrowDamage;
    private final Map<Role, Double> arrowDamage = new EnumMap<>(Role.class);
    private final Set<Role> onlyWhilePrepared = EnumSet.noneOf(Role.class);

    @Inject
    public RoleBowService(Champions champions) {
        loadConfig(champions.getConfig());
    }

    /**
     * The damage a fully drawn arrow lands for when nothing more specific applies - a mob's arrow, or a
     * class that has no bow damage of its own.
     */
    public double getBaseArrowDamage() {
        return baseArrowDamage;
    }

    /**
     * The damage a fully drawn arrow from this class lands for.
     *
     * @param role the class that loosed the arrow, or null if it was not loosed by one
     * @return the class's arrow damage, or the base arrow damage when it has none of its own
     */
    public double getArrowDamage(@Nullable Role role) {
        return role == null ? baseArrowDamage : arrowDamage.getOrDefault(role, baseArrowDamage);
    }

    /**
     * Whether this class's bow is only a delivery tool for its bow skills, refusing to fire without one
     * prepared.
     *
     * @param role the class to check
     * @return whether the class must have a bow skill prepared to shoot
     */
    public boolean isOnlyWhilePrepared(@NotNull Role role) {
        return onlyWhilePrepared.contains(role);
    }

    @Override
    public void loadConfig(@NotNull ExtendedYamlConfiguration config) {
        this.baseArrowDamage = config.getOrSaveObject("combat.arrow-base-damage", 6.0, Double.class);

        arrowDamage.clear();
        onlyWhilePrepared.clear();
        for (Role role : Role.values()) {
            if (!role.isUsesBow()) continue;

            final String path = "class." + role.name().toLowerCase() + ".bow.";
            arrowDamage.put(role, config.getOrSaveObject(path + "arrowDamage", baseArrowDamage, Double.class));
            if (config.getOrSaveBoolean(path + "onlyWhilePrepared", false)) {
                onlyWhilePrepared.add(role);
            }
        }
    }
}
