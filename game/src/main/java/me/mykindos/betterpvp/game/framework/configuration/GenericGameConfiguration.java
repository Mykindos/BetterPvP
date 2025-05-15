package me.mykindos.betterpvp.game.framework.configuration;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import me.mykindos.betterpvp.game.framework.model.attribute.GameAttributeManager;
import me.mykindos.betterpvp.game.framework.model.attribute.bound.AllowLateJoinsAttribute;
import me.mykindos.betterpvp.game.framework.model.attribute.bound.GameDurationAttribute;
import me.mykindos.betterpvp.game.framework.model.attribute.bound.MaxPlayersAttribute;
import me.mykindos.betterpvp.game.framework.model.attribute.bound.RequiredPlayersAttribute;
import me.mykindos.betterpvp.game.framework.model.attribute.bound.RespawnTimerAttribute;
import me.mykindos.betterpvp.game.framework.model.attribute.bound.RespawnsAttribute;
import me.mykindos.betterpvp.game.framework.model.chat.GenericColorProvider;
import me.mykindos.betterpvp.game.framework.model.chat.PlayerColorProvider;
import me.mykindos.betterpvp.game.framework.model.player.PlayerInteractionSettings;
import me.mykindos.betterpvp.game.framework.model.spawnpoint.GenericSpawnPointProvider;
import me.mykindos.betterpvp.game.framework.model.spawnpoint.SpawnPointProvider;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * A generic configuration for a game, which can be extended by specific game types for
 * additional configuration.
 */
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@Getter
@SuperBuilder
public class GenericGameConfiguration {

    @NotNull String name;
    @NotNull String abbreviation;
    @Builder.Default @NotNull SpawnPointProvider spawnPointProvider = new GenericSpawnPointProvider();
    @Builder.Default @NotNull PlayerColorProvider playerColorProvider = new GenericColorProvider();
    @Builder.Default PlayerInteractionSettings interactionSettings = PlayerInteractionSettings.builder().build();

    // Configuration values for attributes
    @Getter(AccessLevel.NONE) @Builder.Default Integer requiredPlayers = null;
    @Getter(AccessLevel.NONE) @Builder.Default Integer maxPlayers = null;
    @Getter(AccessLevel.NONE) @Builder.Default Boolean respawns = null;
    @Getter(AccessLevel.NONE) @Builder.Default Double respawnTimer = null;
    @Getter(AccessLevel.NONE) @Builder.Default Duration duration = null;
    @Getter(AccessLevel.NONE) @Builder.Default Boolean allowLateJoins = null;


    // Attributes
    private transient RequiredPlayersAttribute requiredPlayersAttribute;
    private transient MaxPlayersAttribute maxPlayersAttribute;
    private transient RespawnsAttribute respawnsAttribute;
    private transient RespawnTimerAttribute respawnTimerAttribute;
    private transient GameDurationAttribute gameDurationAttribute;
    private transient AllowLateJoinsAttribute allowLateJoinsAttribute;

    @Inject
    public void setAttributes(
            RequiredPlayersAttribute requiredPlayersAttribute,
            MaxPlayersAttribute maxPlayersAttribute,
            RespawnsAttribute respawnsAttribute,
            RespawnTimerAttribute respawnTimerAttribute,
            GameDurationAttribute gameDurationAttribute,
            AllowLateJoinsAttribute allowLateJoinsAttribute,
            GameAttributeManager attributeManager) {
        this.requiredPlayersAttribute = requiredPlayersAttribute;
        this.maxPlayersAttribute = maxPlayersAttribute;
        this.respawnsAttribute = respawnsAttribute;
        this.respawnTimerAttribute = respawnTimerAttribute;
        this.gameDurationAttribute = gameDurationAttribute;
        this.allowLateJoinsAttribute = allowLateJoinsAttribute;

        // Register attributes
        attributeManager.registerAttribute(requiredPlayersAttribute);
        attributeManager.registerAttribute(maxPlayersAttribute);
        attributeManager.registerAttribute(respawnsAttribute);
        attributeManager.registerAttribute(respawnTimerAttribute);
        attributeManager.registerAttribute(gameDurationAttribute);
        attributeManager.registerAttribute(allowLateJoinsAttribute);

        // Set attribute values from configuration
        if (requiredPlayers != null) {
            requiredPlayersAttribute.setValue(requiredPlayers);
        }
        if (maxPlayers != null) {
            maxPlayersAttribute.setValue(maxPlayers);
        }
        if (respawns != null) {
            respawnsAttribute.setValue(respawns);
        }
        if (respawnTimer != null) {
            respawnTimerAttribute.setValue(respawnTimer);
        }
        if (duration != null) {
            gameDurationAttribute.setValue(duration);
        }

        if (allowLateJoins != null) {
            allowLateJoinsAttribute.setValue(allowLateJoins);
        }
    }

    public void validate() {
        Preconditions.checkArgument(!name.isEmpty(), "name cannot be empty");
    }
}
