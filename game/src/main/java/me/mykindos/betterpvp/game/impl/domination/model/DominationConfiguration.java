package me.mykindos.betterpvp.game.impl.domination.model;

import com.google.inject.Inject;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import me.mykindos.betterpvp.game.framework.configuration.TeamGameConfiguration;
import me.mykindos.betterpvp.game.framework.model.attribute.GameAttributeManager;
import me.mykindos.betterpvp.game.framework.model.attribute.bound.GameDurationAttribute;
import me.mykindos.betterpvp.game.impl.domination.model.attribute.CapturePointScoreAttribute;
import me.mykindos.betterpvp.game.impl.domination.model.attribute.GemScoreAttribute;
import me.mykindos.betterpvp.game.impl.domination.model.attribute.KillScoreAttribute;
import me.mykindos.betterpvp.game.impl.domination.model.attribute.ScoreToWinAttribute;
import me.mykindos.betterpvp.game.impl.domination.model.attribute.SecondsToCaptureAttribute;

@Getter
@SuperBuilder
public class DominationConfiguration extends TeamGameConfiguration {

    // Configuration values for attributes
    @Getter(AccessLevel.NONE) @Builder.Default private Integer scoreToWin = null;
    @Getter(AccessLevel.NONE) @Builder.Default private Integer capturePointScore = null;
    @Getter(AccessLevel.NONE) @Builder.Default private Integer killScore = null;
    @Getter(AccessLevel.NONE) @Builder.Default private Integer gemScore = null;
    @Getter(AccessLevel.NONE) @Builder.Default private Float secondsToCapture = null;

    // Attributes
    private transient ScoreToWinAttribute scoreToWinAttribute;
    private transient CapturePointScoreAttribute capturePointScoreAttribute;
    private transient KillScoreAttribute killScoreAttribute;
    private transient GemScoreAttribute gemScoreAttribute;
    private transient SecondsToCaptureAttribute secondsToCaptureAttribute;

    @Inject
    public void setAttributes(
            ScoreToWinAttribute scoreToWinAttribute,
            CapturePointScoreAttribute capturePointScoreAttribute,
            KillScoreAttribute killScoreAttribute,
            GemScoreAttribute gemScoreAttribute,
            SecondsToCaptureAttribute secondsToCaptureAttribute,
            GameAttributeManager attributeManager) {
        this.scoreToWinAttribute = scoreToWinAttribute;
        this.capturePointScoreAttribute = capturePointScoreAttribute;
        this.killScoreAttribute = killScoreAttribute;
        this.gemScoreAttribute = gemScoreAttribute;
        this.secondsToCaptureAttribute = secondsToCaptureAttribute;

        // Register attributes
        attributeManager.registerAttribute(scoreToWinAttribute);
        attributeManager.registerAttribute(capturePointScoreAttribute);
        attributeManager.registerAttribute(killScoreAttribute);
        attributeManager.registerAttribute(gemScoreAttribute);
        attributeManager.registerAttribute(secondsToCaptureAttribute);

        // Set attribute values from configuration
        if (scoreToWin != null) {
            scoreToWinAttribute.setValue(scoreToWin);
        }
        if (capturePointScore != null) {
            capturePointScoreAttribute.setValue(capturePointScore);
        }
        if (killScore != null) {
            killScoreAttribute.setValue(killScore);
        }
        if (gemScore != null) {
            gemScoreAttribute.setValue(gemScore);
        }
        if (secondsToCapture != null) {
            secondsToCaptureAttribute.setValue(secondsToCapture);
        }
    }

}
