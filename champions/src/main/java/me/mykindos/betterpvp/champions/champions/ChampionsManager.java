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
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.energy.EnergyHandler;

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
    private final EnergyHandler energy;
    private final ThrowableHandler throwables;

    @Inject
    public ChampionsManager(ClientManager clientManager, ChampionsSkillManager skills, RoleManager roles, BuildManager builds,
                            CooldownManager cooldowns, EffectManager effects, EnergyHandler energy, ThrowableHandler throwables) {
        this.clientManager = clientManager;
        this.skills = skills;
        this.roles = roles;
        this.builds = builds;
        this.cooldowns = cooldowns;
        this.effects = effects;
        this.energy = energy;
        this.throwables = throwables;
    }
}
