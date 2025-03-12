package me.mykindos.betterpvp.game.impl.ctf;

import com.google.inject.Binder;
import me.mykindos.betterpvp.game.guice.GameModule;
import org.bukkit.event.Listener;

import java.util.Set;

public class CaptureTheFlagModule implements GameModule {
    @Override
    public String getId() {
        return "CTF";
    }

    @Override
    public Set<Class<? extends Listener>> getListeners() {
        return Set.of(
        );
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {

    }

    @Override
    public void configure(Binder binder) {

    }
}
