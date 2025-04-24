package me.mykindos.betterpvp.game.impl.domination;

import me.mykindos.betterpvp.game.framework.AbstractGame;
import me.mykindos.betterpvp.game.framework.TeamGame;
import me.mykindos.betterpvp.game.guice.AbstractGameModule;
import me.mykindos.betterpvp.game.impl.domination.controller.GameController;
import me.mykindos.betterpvp.game.impl.domination.listener.DominationListener;
import me.mykindos.betterpvp.game.impl.domination.listener.DominationSidebarListener;
import me.mykindos.betterpvp.game.impl.domination.model.DominationConfiguration;

public class DominationModule extends AbstractGameModule {

    private final Domination game;

    public DominationModule(Domination game) {
        super("DOM");
        this.game = game;

        registerListener(DominationListener.class);
        registerListener(DominationSidebarListener.class);
    }

    @Override
    protected void configure() {
        super.configure();
        bind(Domination.class).toInstance(game);
        bind(DominationConfiguration.class).toInstance(game.getConfiguration());
        bind(AbstractGame.class).toInstance(game);
        bind(TeamGame.class).toInstance(game);
        bind(GameController.class).asEagerSingleton();
    }
}