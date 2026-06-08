package me.mykindos.betterpvp.core.locale;

import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.plugin.java.JavaPlugin;

public final class TranslationService {

    private static final BetterPvPTranslator TRANSLATOR = new BetterPvPTranslator();
    private static volatile boolean registered;

    private TranslationService() {
    }

    public static void registerGlobalTranslator() {
        if (!registered) {
            GlobalTranslator.translator().addSource(TRANSLATOR);
            registered = true;
        }
    }

    public static void registerBundle(JavaPlugin plugin, String baseName) {
        TRANSLATOR.registerBundle(plugin.getClass().getClassLoader(), baseName);
    }

    public static BetterPvPTranslator translator() {
        return TRANSLATOR;
    }
}