package me.mykindos.betterpvp.game.impl.ctf.model;

import com.google.inject.Inject;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import me.mykindos.betterpvp.game.framework.configuration.TeamGameConfiguration;
import me.mykindos.betterpvp.game.framework.model.attribute.GameAttributeManager;
import me.mykindos.betterpvp.game.impl.ctf.model.attribute.ScoreToWinAttribute;
import me.mykindos.betterpvp.game.impl.ctf.model.attribute.SuddenDeathDurationAttribute;

import java.time.Duration;

@Getter
@SuperBuilder
public class CTFConfiguration extends TeamGameConfiguration {

    // Configuration values for attributes
    @Getter(AccessLevel.NONE) @Builder.Default Integer scoreToWin = null;
    @Getter(AccessLevel.NONE) @Builder.Default Duration suddenDeathDuration = null;

    // Attributes
    private transient ScoreToWinAttribute scoreToWinAttribute;
    private transient SuddenDeathDurationAttribute suddenDeathDurationAttribute;

    @Inject
    public void setAttributes(
            ScoreToWinAttribute scoreToWinAttribute,
            SuddenDeathDurationAttribute suddenDeathDurationAttribute,
            GameAttributeManager attributeManager) {
        this.scoreToWinAttribute = scoreToWinAttribute;
        this.suddenDeathDurationAttribute = suddenDeathDurationAttribute;

        // Register attributes
        attributeManager.registerAttribute(scoreToWinAttribute);
        attributeManager.registerAttribute(suddenDeathDurationAttribute);

        // Set attribute values from configuration
        if (scoreToWin != null) {
            scoreToWinAttribute.setValue(scoreToWin);
        }
        if (suddenDeathDuration != null) {
            suddenDeathDurationAttribute.setValue(suddenDeathDuration);
        }
    }
}
