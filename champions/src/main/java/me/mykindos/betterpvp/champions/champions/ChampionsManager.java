package me.mykindos.betterpvp.champions.champions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableHandler;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.displayname.DisplayNameProvider;
import me.mykindos.betterpvp.core.displayname.DisplayNameService;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.energy.EnergyService;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;

import java.awt.*;

/**
 * A wrapper containing frequently used dependencies throughout the champions module
 */
@Singleton
@Getter
public class ChampionsManager {

    private final ClientManager clientManager;
    private final ChampionsSkillManager skills;
    private final RoleManager roles;
    private final BuildManager builds;
    private final CooldownManager cooldowns;
    private final EffectManager effects;
    private final EnergyService energy;
    private final ThrowableHandler throwables;
    private final DisplayNameService displayNameService;

    @Inject
    public ChampionsManager(ClientManager clientManager, ChampionsSkillManager skills, RoleManager roles, BuildManager builds,
                            CooldownManager cooldowns, EffectManager effects, EnergyService energy, ThrowableHandler throwables,
                            DisplayNameService displayNameService) {
        this.clientManager = clientManager;
        this.skills = skills;
        this.roles = roles;
        this.builds = builds;
        this.cooldowns = cooldowns;
        this.effects = effects;
        this.energy = energy;
        this.throwables = throwables;
        this.displayNameService = displayNameService;
    }

    /**
     * Convenience method for retrieving an entity's display name as a {@link Component}.
     * This is a shorter way of accessing the {@link DisplayNameService} and its
     * {@link DisplayNameProvider} directly.
     *
     * @param entity the entity whose display name should be retrieved
     * @param viewer the entity viewing the display name, used for contextual formatting
     * @return the entity's display name as a {@link Component}
     */
    public Component getDisplayNameAsComponent(Entity entity, Entity viewer) {
        return this.displayNameService.getProvider().getDisplayNameAsComponent(entity, viewer);
    }
}
