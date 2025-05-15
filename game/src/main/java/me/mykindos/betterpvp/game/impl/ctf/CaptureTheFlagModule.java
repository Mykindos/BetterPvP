package me.mykindos.betterpvp.game.impl.ctf;

import me.mykindos.betterpvp.game.framework.AbstractGame;
import me.mykindos.betterpvp.game.framework.TeamGame;
import me.mykindos.betterpvp.game.guice.AbstractGameModule;
import me.mykindos.betterpvp.game.impl.ctf.controller.GameController;
import me.mykindos.betterpvp.game.impl.ctf.listener.CTFSidebarListener;
import me.mykindos.betterpvp.game.impl.ctf.listener.FlagHolderListener;
import me.mykindos.betterpvp.game.impl.ctf.listener.FlagInteractionListener;
import me.mykindos.betterpvp.game.impl.ctf.listener.FlagTicker;
import me.mykindos.betterpvp.game.impl.ctf.listener.SuddenDeathListener;
import me.mykindos.betterpvp.game.impl.ctf.model.CTFConfiguration;

public class CaptureTheFlagModule extends AbstractGameModule {

    private final CaptureTheFlag game;

    public CaptureTheFlagModule(CaptureTheFlag game) {
        super("CTF");
        this.game = game;
        registerListener(FlagInteractionListener.class);
        registerListener(FlagHolderListener.class);
        registerListener(SuddenDeathListener.class);
        registerListener(CTFSidebarListener.class);
        registerListener(FlagTicker.class);
    }

    @Override
    protected void configure() {
        super.configure();
        bind(CaptureTheFlag.class).toInstance(game);
        bind(CTFConfiguration.class).toInstance(game.getConfiguration());
        bind(AbstractGame.class).toInstance(game);
        bind(TeamGame.class).toInstance(game);
        bind(GameController.class).asEagerSingleton();
    }
}