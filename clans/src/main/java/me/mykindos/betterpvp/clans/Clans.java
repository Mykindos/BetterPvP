package me.mykindos.betterpvp.clans;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import java.lang.reflect.Field;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.clans.achievements.loader.ClansAchievementCategoryLoader;
import me.mykindos.betterpvp.clans.achievements.loader.ClansAchievementLoader;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.explosion.ExplosiveResistanceBootstrap;
import me.mykindos.betterpvp.clans.commands.ClansCommandLoader;
import me.mykindos.betterpvp.clans.display.ClansSidebarListener;
import me.mykindos.betterpvp.clans.injector.ClansInjectorModule;
import me.mykindos.betterpvp.clans.leaderboards.ClansLeaderboardLoader;
import me.mykindos.betterpvp.clans.listener.ClansListenerLoader;
import me.mykindos.betterpvp.clans.tips.ClansTipLoader;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.config.ConfigInjectorModule;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.CurrentMode;
import me.mykindos.betterpvp.core.framework.ModuleLoadedEvent;
import me.mykindos.betterpvp.core.framework.adapter.Adapters;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapters;
import me.mykindos.betterpvp.core.framework.sidebar.Sidebar;
import me.mykindos.betterpvp.core.framework.sidebar.SidebarController;
import me.mykindos.betterpvp.core.framework.sidebar.SidebarType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEventExecutor;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemLoader;
import me.mykindos.betterpvp.core.item.component.impl.uuid.UUIDManager;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

@Singleton
public class Clans extends BPvPPlugin {

    private final String PACKAGE = getClass().getPackageName();

    @Getter
    @Setter
    private Injector injector;

    @Inject
    private Database database;

    @Inject
    private UpdateEventExecutor updateEventExecutor;

    private ClanManager clanManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        var core = (Core) Bukkit.getPluginManager().getPlugin("Core");
        if (core != null) {

            core.setCurrentMode(CurrentMode.CLANS);

            Reflections reflections = new Reflections(PACKAGE, Scanners.FieldsAnnotated);
            Set<Field> fields = reflections.getFieldsAnnotatedWith(Config.class);

            injector = core.getInjector().createChildInjector(new ClansInjectorModule(this),
                    new ConfigInjectorModule(this, fields));
            injector.injectMembers(this);

            database.getConnection().runDatabaseMigrations(getClass().getClassLoader(), "classpath:clans-migrations/postgres/", "clans");

            Bukkit.getPluginManager().callEvent(new ModuleLoadedEvent("Clans"));

            var listenerLoader = injector.getInstance(ClansListenerLoader.class);
            listenerLoader.registerListeners(PACKAGE);

            var clansCommandLoader = injector.getInstance(ClansCommandLoader.class);
            clansCommandLoader.loadCommands(PACKAGE);

            var clanTipManager = injector.getInstance(ClansTipLoader.class);
            clanTipManager.loadTips(PACKAGE);

            clanManager = injector.getInstance(ClanManager.class);
            clanManager.loadFromList(clanManager.getRepository().getAll());

            var clansSidebar = injector.getInstance(ClansSidebarListener.class);
            var sidebarController = injector.getInstance(SidebarController.class);
            if (clansSidebar.isEnabled()) {
                sidebarController.setDefaultProvider(gamer -> new Sidebar(gamer, getConfig().getString("server.sidebar.title"), SidebarType.GENERAL));
            }

            var leaderboardLoader = injector.getInstance(ClansLeaderboardLoader.class);
            leaderboardLoader.registerLeaderboards(PACKAGE);

            var clansAchievementCategoryLoader = injector.getInstance(ClansAchievementCategoryLoader.class);
            clansAchievementCategoryLoader.loadAchievementCategories(PACKAGE);

            var clansAchievementLoader = injector.getInstance(ClansAchievementLoader.class);
            clansAchievementLoader.loadAchievements(PACKAGE);

            updateEventExecutor.loadPlugin(this);

            var uuidManager = injector.getInstance(UUIDManager.class);
            uuidManager.loadObjectsFromNamespace("clans");

            final Adapters adapters = new Adapters(this);
            final Reflections reflectionAdapters = new Reflections(PACKAGE);
            adapters.loadAdapters(reflectionAdapters.getTypesAnnotatedWith(PluginAdapter.class));
            adapters.loadAdapters(reflectionAdapters.getTypesAnnotatedWith(PluginAdapters.class));

            final ItemLoader itemLoader = new ItemLoader(this);
            itemLoader.load(adapters, reflectionAdapters.getTypesAnnotatedWith(ItemKey.class));
            this.registerItems();

            clearCraftingRecipes();
        }
    }

    private void registerItems() {
        this.injector.getInstance(ExplosiveResistanceBootstrap.class).register();
    }

    private void clearCraftingRecipes() {
        CraftingRecipeRegistry recipeRegistry = injector.getInstance(CraftingRecipeRegistry.class);
        recipeRegistry.clearRecipe(NamespacedKey.fromString("minecraft:oak_boat"));
        recipeRegistry.clearRecipe(NamespacedKey.fromString("minecraft:spruce_boat"));
        recipeRegistry.clearRecipe(NamespacedKey.fromString("minecraft:jungle_boat"));
        recipeRegistry.clearRecipe(NamespacedKey.fromString("minecraft:acacia_boat"));
        recipeRegistry.clearRecipe(NamespacedKey.fromString("minecraft:dark_oak_boat"));
        recipeRegistry.clearRecipe(NamespacedKey.fromString("minecraft:mangrove_boat"));
        recipeRegistry.clearRecipe(NamespacedKey.fromString("minecraft:cherry_boat"));
        recipeRegistry.clearRecipe(NamespacedKey.fromString("minecraft:pale_oak_boat"));
    }

    @Override
    public void onDisable() {
        clanManager.getRepository().processPropertyUpdates(false);
    }
}
