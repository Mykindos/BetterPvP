package me.mykindos.betterpvp.core.command.brigadier;

import com.google.inject.Injector;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.handler.LifecycleEventHandler;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEventType;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.brigadier.CommandFixtures.InfoCommand;
import me.mykindos.betterpvp.core.command.brigadier.CommandFixtures.RootCommand;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import static me.mykindos.betterpvp.core.command.brigadier.CommandFixtures.config;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Loading Brigadier commands into the server")
class BrigadierCommandLoaderTest {

    private final CommandFixtures fixtures = new CommandFixtures();
    private final BPvPPlugin plugin = mock(BPvPPlugin.class);
    private final Injector injector = mock(Injector.class);
    private final BrigadierCommandManager manager = new BrigadierCommandManager();
    @SuppressWarnings("unchecked")
    private final LifecycleEventManager<Plugin> lifecycle = mock(LifecycleEventManager.class);
    private final Commands registrar = mock(Commands.class);
    private BrigadierCommandLoader loader;

    @BrigadierSubCommand(RootCommand.class)
    static class AnnotatedInfoCommand extends InfoCommand {
        AnnotatedInfoCommand(CommandFixtures fixtures) {
            super(fixtures.clientManager);
        }
    }

    static class TranslatedRootCommand extends RootCommand {
        TranslatedRootCommand(CommandFixtures fixtures) {
            super(fixtures.clientManager);
        }

        @Override
        public Component getDescriptionComponent() {
            return Translations.component("core.command.channel.description");
        }
    }

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        when(plugin.getInjector()).thenReturn(injector);
        when(plugin.getLifecycleManager()).thenReturn(lifecycle);
        loader = new BrigadierCommandLoader(plugin);
        final Field field = BrigadierCommandLoader.class.getDeclaredField("brigadierCommandManager");
        field.setAccessible(true);
        field.set(loader, manager);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void runCommandsLifecycleEvent() {
        final ArgumentCaptor<LifecycleEventHandler> handler = ArgumentCaptor.forClass(LifecycleEventHandler.class);
        verify(lifecycle).registerEventHandler(any(LifecycleEventType.class), handler.capture());
        final ReloadableRegistrarEvent<Commands> event = mock(ReloadableRegistrarEvent.class);
        when(event.registrar()).thenReturn(registrar);
        handler.getValue().run(event);
    }

    private RootCommand loadRoot(ExtendedYamlConfiguration... configs) {
        final RootCommand root = new RootCommand(fixtures.clientManager);
        when(injector.getInstance(RootCommand.class)).thenReturn(root);
        when(plugin.getConfig("permissions/commands")).thenReturn(configs[0], Arrays.copyOfRange(configs, 1, configs.length));
        loader.load(RootCommand.class);
        runCommandsLifecycleEvent();
        return root;
    }

    @Test
    @DisplayName("AC1 a top-level command is registered under its name and aliases with its description")
    @SuppressWarnings("unchecked")
    void ac1_registeredWithNameAliasesAndDescription() {
        final RootCommand root = loadRoot(config("root.requiredRank=PLAYER"));

        final ArgumentCaptor<LiteralCommandNode<CommandSourceStack>> node = ArgumentCaptor.forClass(LiteralCommandNode.class);
        final ArgumentCaptor<Collection<String>> aliases = ArgumentCaptor.forClass(Collection.class);
        verify(registrar).register(node.capture(), eq("Root command"), aliases.capture());
        assertEquals("root", node.getValue().getName());
        assertEquals(Set.of("r"), Set.copyOf(aliases.getValue()));
        assertSame(root, manager.getObject("root").orElseThrow());
        assertSame(root, manager.getObject("r").orElseThrow());
    }

    @Test
    @DisplayName("AC28 a translatable description is registered as its English text")
    void ac28_translatedDescriptionRegisteredInEnglish() {
        EnglishCommandMessages.registerBundle();
        final TranslatedRootCommand root = new TranslatedRootCommand(fixtures);
        when(injector.getInstance(TranslatedRootCommand.class)).thenReturn(root);
        when(plugin.getConfig("permissions/commands")).thenReturn(config("root.requiredRank=PLAYER"));

        loader.load(TranslatedRootCommand.class);
        runCommandsLifecycleEvent();

        verify(registrar).register(any(LiteralCommandNode.class), eq("Change chat channel"), anyCollection());
    }

    @Test
    @DisplayName("AC2 a class annotated @BrigadierSubCommand becomes a child of its parent")
    void ac2_annotatedSubcommandAttachedToParent() {
        final RootCommand root = new RootCommand(fixtures.clientManager);
        final AnnotatedInfoCommand info = new AnnotatedInfoCommand(fixtures);
        when(injector.getInstance(RootCommand.class)).thenReturn(root);
        when(injector.getInstance(AnnotatedInfoCommand.class)).thenReturn(info);

        loader.loadSubCommands(Set.of(AnnotatedInfoCommand.class));

        assertTrue(root.getChildren().contains(info));
        assertSame(root, info.getParent());
    }

    @Test
    @DisplayName("AC5 a config reload applies a changed rank or enabled flag without a restart")
    void ac5_reloadAppliesChangedConfig() {
        final RootCommand root = loadRoot(config("root.requiredRank=PLAYER"),
                config("root.requiredRank=MODERATOR", "root.enabled=false"));
        final CommandSourceStack admin = fixtures.source(fixtures.player("Admin", Rank.ADMIN, false));
        assertEquals(Rank.PLAYER, root.getRequiredRank());

        loader.reload();

        assertEquals(Rank.MODERATOR, root.getRequiredRank());
        assertFalse(root.requirement(admin));
    }
}
