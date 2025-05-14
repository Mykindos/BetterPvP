package me.mykindos.betterpvp.game.framework.configuration;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.time.Duration;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.SuperBuilder;
import me.mykindos.betterpvp.game.framework.model.attribute.GameAttributeManager;
import me.mykindos.betterpvp.game.framework.model.attribute.team.AllowOversizedTeamsAttribute;
import me.mykindos.betterpvp.game.framework.model.attribute.team.AutoBalanceOnDeathAttribute;
import me.mykindos.betterpvp.game.framework.model.attribute.team.DurationBeforeAutoBalanceAttribute;
import me.mykindos.betterpvp.game.framework.model.attribute.team.ForceBalanceAttribute;
import me.mykindos.betterpvp.game.framework.model.attribute.team.KeepSameTeamAttribute;
import me.mykindos.betterpvp.game.framework.model.attribute.team.MaxImbalanceAttribute;
import me.mykindos.betterpvp.game.framework.model.team.GenericTeamBalancerProvider;
import me.mykindos.betterpvp.game.framework.model.team.TeamBalancerProvider;
import me.mykindos.betterpvp.game.framework.model.team.TeamProperties;
import org.jetbrains.annotations.NotNull;

/**
 * A configuration for a team-based game
 */
@Getter
@SuperBuilder
public class TeamGameConfiguration extends GenericGameConfiguration {

    @Singular
    private final @NotNull Set<@NotNull TeamProperties> teamProperties;

    @Builder.Default @NotNull TeamBalancerProvider teamBalancerProvider = new GenericTeamBalancerProvider();

    @Getter(AccessLevel.NONE) @Builder.Default Integer maxImbalance = null;
    @Getter(AccessLevel.NONE) @Builder.Default Duration durationBeforeAutoBalance = null;
    @Getter(AccessLevel.NONE) @Builder.Default Boolean autoBalanceOnDeath = null;
    @Getter(AccessLevel.NONE) @Builder.Default Boolean forceBalance = null;
    @Getter(AccessLevel.NONE) @Builder.Default Boolean allowOversizedTeams = null;
    @Getter(AccessLevel.NONE) @Builder.Default Boolean keepSameTeams = null;


    private transient MaxImbalanceAttribute maxImbalanceAttribute;
    private transient DurationBeforeAutoBalanceAttribute durationBeforeAutoBalanceAttribute;
    private transient AutoBalanceOnDeathAttribute autoBalanceOnDeathAttribute;
    private transient ForceBalanceAttribute forceBalanceAttribute;
    private transient AllowOversizedTeamsAttribute allowOversizedTeamsAttribute;
    private transient KeepSameTeamAttribute keepSameTeamAttribute;


    @Inject
    public void setAttributes(
            MaxImbalanceAttribute maxImbalanceAttribute,
            DurationBeforeAutoBalanceAttribute durationBeforeAutoBalanceAttribute,
            AutoBalanceOnDeathAttribute autoBalanceOnDeathAttribute,
            ForceBalanceAttribute forceBalanceAttribute,
            AllowOversizedTeamsAttribute allowOversizedTeamsAttribute,
            KeepSameTeamAttribute keepSameTeamAttribute,
            GameAttributeManager gameAttributeManager
    ) {
        this.maxImbalanceAttribute = maxImbalanceAttribute;
        this.durationBeforeAutoBalanceAttribute = durationBeforeAutoBalanceAttribute;
        this.autoBalanceOnDeathAttribute = autoBalanceOnDeathAttribute;
        this.forceBalanceAttribute = forceBalanceAttribute;
        this.allowOversizedTeamsAttribute = allowOversizedTeamsAttribute;
        this.keepSameTeamAttribute = keepSameTeamAttribute;

        gameAttributeManager.registerAttribute(maxImbalanceAttribute);
        gameAttributeManager.registerAttribute(durationBeforeAutoBalanceAttribute);
        gameAttributeManager.registerAttribute(autoBalanceOnDeathAttribute);
        gameAttributeManager.registerAttribute(forceBalanceAttribute);
        gameAttributeManager.registerAttribute(allowOversizedTeamsAttribute);
        gameAttributeManager.registerAttribute(keepSameTeamAttribute);

        if (maxImbalance != null) {
            maxImbalanceAttribute.setValue(maxImbalance);
        }

        if (durationBeforeAutoBalance != null) {
            durationBeforeAutoBalanceAttribute.setValue(durationBeforeAutoBalance);
        }

        if (autoBalanceOnDeath != null) {
            autoBalanceOnDeathAttribute.setValue(autoBalanceOnDeath);
        }

        if (forceBalance != null) {
            forceBalanceAttribute.setValue(forceBalance);
        }

        if (allowOversizedTeams != null) {
            allowOversizedTeamsAttribute.setValue(allowOversizedTeams);
        }
        if (keepSameTeams != null) {
            keepSameTeamAttribute.setValue(keepSameTeams);
        }
    }

    @Override
    public void validate() {
        Preconditions.checkArgument(teamProperties.size() >= 2, "Must have at least 2 teams");
        super.validate();
    }

}
