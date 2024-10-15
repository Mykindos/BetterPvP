package me.mykindos.betterpvp.core.world;

import com.mineplex.studio.sdk.modules.game.BuiltInGameState;
import com.mineplex.studio.sdk.modules.game.MineplexGameMechanicFactory;
import com.mineplex.studio.sdk.modules.game.SingleWorldMineplexGame;
import com.mineplex.studio.sdk.modules.game.helper.GameStateTracker;
import com.mineplex.studio.sdk.modules.game.helper.PlayerStateTracker;
import com.mineplex.studio.sdk.modules.world.MineplexWorld;
import lombok.NonNull;
import lombok.experimental.Delegate;

public class BetterPVPMineplexGame implements SingleWorldMineplexGame {

    @Delegate
    private final GameStateTracker gameStateTracker = new GameStateTracker(this, BuiltInGameState.STARTED);

    @Delegate
    private final PlayerStateTracker playerStateTracker = new PlayerStateTracker(this);


    @Override
    public @NonNull MineplexWorld getGameWorld() {
        return null;
    }

    @Override
    public @NonNull String getName() {
        return "Clans";
    }

    @Override
    public @NonNull MineplexGameMechanicFactory getGameMechanicFactory() {
        return null;
    }

    @Override
    public void setup() {

    }

    @Override
    public void teardown() {

    }
}
