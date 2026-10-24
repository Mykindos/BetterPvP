package me.mykindos.betterpvp.lunar.commands.loader;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierCommandLoader;
import me.mykindos.betterpvp.lunar.Lunar;

@Singleton
public class BrigadierLunarCommandLoader extends BrigadierCommandLoader {
    @Inject
    public BrigadierLunarCommandLoader(Lunar plugin) {
        super(plugin);
    }
}
