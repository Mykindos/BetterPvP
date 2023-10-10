package me.mykindos.betterpvp.core;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.client.ClientManager;
import me.mykindos.betterpvp.core.command.loader.CoreCommandLoader;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.config.ConfigInjectorModule;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.injector.DatabaseInjectorModule;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.updater.UpdateEventExecutor;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.injector.CoreInjectorModule;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.loader.CoreListenerLoader;
import net.kyori.adventure.key.Key;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.Field;
import java.util.Set;

import static io.papermc.paper.network.ChannelInitializeListenerHolder.hasListener;
import static io.papermc.paper.network.ChannelInitializeListenerHolder.removeListener;

@Slf4j
public class Core extends BPvPPlugin {

    private final String PACKAGE = getClass().getPackageName();
    private static final Key listenerKey = Key.key("unsafechat", "listener");

    @Getter
    @Setter
    private Injector injector;

    @Inject
    private Database database;

    private ClientManager clientManager;
    private GamerManager gamerManager;

    @Inject
    private UpdateEventExecutor updateEventExecutor;

    @Inject
    @Config(path = "core.database.prefix")
    @Getter
    private String databasePrefix;

    @Inject
    @Config(path = "core.password", defaultValue = "")
    private String password;

    public void onEnable() {
        saveDefaultConfig();

        Reflections reflections = new Reflections(PACKAGE, Scanners.FieldsAnnotated);
        Set<Field> fields = reflections.getFieldsAnnotatedWith(Config.class);

        injector = Guice.createInjector(new CoreInjectorModule(this),
                new DatabaseInjectorModule(),
                new ConfigInjectorModule(this, fields));
        injector.injectMembers(this);

        database.getConnection().runDatabaseMigrations(getClass().getClassLoader(), "classpath:core-migrations", "");

        var coreListenerLoader = injector.getInstance(CoreListenerLoader.class);
        coreListenerLoader.registerListeners(PACKAGE);

        var coreCommandLoader = injector.getInstance(CoreCommandLoader.class);
        coreCommandLoader.loadCommands(PACKAGE);

        clientManager = injector.getInstance(ClientManager.class);
        clientManager.loadFromList(clientManager.getRepository().getAll());

        gamerManager = injector.getInstance(GamerManager.class);
        gamerManager.loadFromList(gamerManager.getGamerRepository().getAll());

        var itemHandler = injector.getInstance(ItemHandler.class);
        itemHandler.loadItemData("Core");

        updateEventExecutor.loadPlugin(this);
        updateEventExecutor.initialize();

        if(password.equals("")) {
            log.error("""
                    
                    In the world of settings, I should be found,
                    But I'm missing, causing a puzzling sound.
                    Without me, things are chaotic, you see,
                    A configuration gone, a mystery to be.
                                        
                    I'm like a piece in a jigsaw puzzle game,
                    Without me, things will never be the same.
                    In the system's heart, where I should reside,
                    I'm absent now, causing strife far and wide.
                                        
                    What am I, this absence so dire,
                    Leaving systems and users to inquire?
                    
                    """);
            getServer().getPluginManager().disablePlugin(this);
        }

    }

    @Override
    public void onDisable() {
        clientManager.getRepository().processStatUpdates(false);
        gamerManager.getGamerRepository().processStatUpdates(false);

        if (hasListener(listenerKey)) {
            removeListener(listenerKey);
        }
    }

}
