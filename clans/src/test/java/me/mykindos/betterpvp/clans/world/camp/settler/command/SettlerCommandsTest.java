package me.mykindos.betterpvp.clans.world.camp.settler.command;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.settler.CampProfessions;
import me.mykindos.betterpvp.clans.world.camp.settler.menu.MenuProbe;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.settler.ProfessionRegistry;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerLeaveReason;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerResult;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.Trait;
import me.mykindos.betterpvp.core.world.settler.TraitGroup;
import me.mykindos.betterpvp.core.world.settler.TraitRegistry;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static me.mykindos.betterpvp.clans.world.camp.settler.menu.MenuProbe.mentions;
import static me.mykindos.betterpvp.clans.world.camp.settler.menu.MenuProbe.text;
import static me.mykindos.betterpvp.clans.world.camp.settler.menu.MenuProbe.told;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SettlerCommandsTest {

    private static final long CLAN = 42;
    private static final SiteKey SITE = Camps.keyFor(CLAN);

    private final SettlerCommands commands = mock(SettlerCommands.class);
    private final SettlerService service = mock(SettlerService.class);
    private final Player player = mock(Player.class);
    private final Clan clan = mock(Clan.class);
    private final Roster roster = new Roster();
    private final ProfessionRegistry professions = new ProfessionRegistry();
    private final TraitRegistry traits = new TraitRegistry();

    @BeforeEach
    void setUp() {
        new CampProfessions(professions);
        traits.register(Trait.builder().id("tireless").key("clans.settler.trait.tireless")
                .group(TraitGroup.BUILDER).build());
        when(clan.getId()).thenReturn(CLAN);
        when(clan.getName()).thenReturn("Wolves");
        when(commands.line(any())).thenAnswer(invocation ->
                Component.text("line " + invocation.<Settler>getArgument(0).getName()));
        doAnswer(invocation -> {
            invocation.<Consumer<Clan>>getArgument(2).accept(clan);
            return null;
        }).when(commands).withCamp(eq(player), eq("Wolves"), any());
        when(service.roster(SITE)).thenReturn(Optional.of(roster));
        when(service.populationCap(SITE)).thenReturn(6);
        when(service.remove(eq(SITE), any(), any())).thenAnswer(invocation ->
                SettlerResult.done(roster.find(invocation.getArgument(1)).orElseThrow()));
    }

    private static Settler settler(String id, String name) {
        final Settler settler = new Settler();
        settler.setId(UUID.fromString(id));
        settler.setName(name);
        settler.setRarity(SettlerRarity.RARE);
        return settler;
    }

    private List<Component> sent() {
        final ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        verify(player, atLeast(1)).sendMessage(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void ac26_eachCommandShowsItsUsageWithTheSettlersPrefix() {
        new SettlerCommand().execute(player, null);
        new SettlerListSubCommand(commands, service).execute(player, null);
        new SettlerDismissSubCommand(commands, service).execute(player, null, "Wolves");

        assertTrue(told(player, "clans.command.settler.usage"));
        assertTrue(told(player, "clans.command.settler.list.usage"));
        assertTrue(told(player, "clans.command.settler.dismiss.usage"));
        assertTrue(sent().stream().allMatch(message -> mentions(message, "clans.prefix.settler")));
        verify(commands, never()).withCamp(any(), any(), any());
    }

    @Test
    void ac27_anUnknownClanIsRefusedByName() {
        final ClanManager clans = mock(ClanManager.class);
        when(clans.getClanByName("Nobody")).thenReturn(Optional.empty());
        final CampStore store = mock(CampStore.class);
        final SettlerCommands real = MenuProbe.build(SettlerCommands.class, clans, store);
        final AtomicReference<Clan> ran = new AtomicReference<>();

        real.withCamp(player, "Nobody", ran::set);

        assertTrue(told(player, "clans.command.settler.no_clan"));
        assertTrue(text(sent().getFirst()).contains("Nobody"));
        assertNull(ran.get());
        verify(store, never()).load(CLAN);
    }

    @Test
    void ac27_theCampIsLoadedBeforeTheSubcommandActsOnTheMainThread() {
        final ClanManager clans = mock(ClanManager.class);
        when(clans.getClanByName("Wolves")).thenReturn(Optional.of(clan));
        final CampStore store = mock(CampStore.class);
        final CompletableFuture<Camp> loading = new CompletableFuture<>();
        when(store.load(CLAN)).thenReturn(loading);
        final SettlerCommands real = MenuProbe.build(SettlerCommands.class, clans, store);
        final AtomicReference<Clan> ran = new AtomicReference<>();

        try (MockedStatic<UtilServer> server = mockStatic(UtilServer.class)) {
            server.when(() -> UtilServer.runTask(any(BPvPPlugin.class), any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        invocation.<Runnable>getArgument(1).run();
                        return null;
                    });

            real.withCamp(player, "Wolves", ran::set);
            assertNull(ran.get(), "not before the camp is loaded");

            loading.complete(null);
            assertSame(clan, ran.get());
            server.verify(() -> UtilServer.runTask(any(BPvPPlugin.class), any(Runnable.class)));
        }
    }

    @Test
    void ac28_listShowsTheCountAndOneLinePerSettler() {
        roster.getSettlers().add(settler("11111111-0000-0000-0000-000000000000", "Aldric Tanner"));
        roster.getSettlers().add(settler("22222222-0000-0000-0000-000000000000", "Mirel of the Fens"));

        new SettlerListSubCommand(commands, service).execute(player, null, "Wolves");

        final List<Component> sent = sent();
        assertEquals(3, sent.size());
        assertTrue(mentions(sent.get(0), "clans.command.settler.list.header"));
        final String header = text(sent.get(0));
        assertTrue(header.contains("Wolves") && header.contains("2") && header.contains("6"));
        assertTrue(text(sent.get(1)).contains("line Aldric Tanner"));
        assertTrue(text(sent.get(2)).contains("line Mirel of the Fens"));
    }

    @Test
    void ac28_anEmptyCampSaysSo() {
        new SettlerListSubCommand(commands, service).execute(player, null, "Wolves");

        assertTrue(told(player, "clans.command.settler.list.empty"));
    }

    @Test
    void ac29_aSettlersLineShowsWhoItIsAndWhatItDoes() {
        final SettlerCommands real = MenuProbe.build(SettlerCommands.class, professions, traits);
        final Settler settler = settler("abcdef12-3456-0000-0000-000000000000", "Tamsin Reed");
        settler.setProfession(CampProfessions.BUILDER);
        settler.setSpecialty(CampProfessions.MASON);
        settler.setTraits(List.of("tireless", "forgotten"));

        final Component line = real.line(settler);

        final String shown = text(line);
        assertTrue(shown.startsWith("Tamsin Reed abcdef12"));
        assertEquals(SettlerRarity.RARE.getColor(), line.color());
        assertTrue(mentions(line, "core.settler.rarity.rare"));
        final String builder = professions.find(CampProfessions.BUILDER).orElseThrow().getKey();
        assertTrue(mentions(line, builder));
        assertTrue(mentions(line, builder + ".specialty." + CampProfessions.MASON));
        assertTrue(mentions(line, "clans.settler.trait.tireless.name"));
        assertTrue(shown.contains("forgotten"), "an unknown trait shows by its id");

        settler.setProfession(null);
        settler.setSpecialty(null);
        assertTrue(mentions(real.line(settler), "clans.command.settler.no_profession"));
    }

    @Test
    void ac30_dismissRemovesTheOneMatchingSettlerAsADismissal() {
        final Settler aldric = settler("abc00000-0000-0000-0000-000000000000", "Aldric Tanner");
        roster.getSettlers().add(aldric);
        roster.getSettlers().add(settler("def00000-0000-0000-0000-000000000000", "Mirel of the Fens"));

        new SettlerDismissSubCommand(commands, service).execute(player, null, "Wolves", "ABC");

        verify(service).remove(SITE, aldric.getId(), SettlerLeaveReason.DISMISSED);
        verify(service, never()).dismiss(any(), any());
        assertTrue(told(player, "clans.command.settler.dismissed"));
    }

    @Test
    void ac30_noMatchOrMoreThanOneIsRefused() {
        roster.getSettlers().add(settler("abc00000-0000-0000-0000-000000000000", "Aldric Tanner"));
        roster.getSettlers().add(settler("abd00000-0000-0000-0000-000000000000", "Mirel of the Fens"));
        final SettlerDismissSubCommand dismiss = new SettlerDismissSubCommand(commands, service);

        dismiss.execute(player, null, "Wolves", "ab");
        dismiss.execute(player, null, "Wolves", "ff");

        verify(service, never()).remove(any(), any(), any());
        assertEquals(2, sent().stream().filter(message -> mentions(message, "clans.command.settler.not_found")).count());
    }

    @Test
    void ac30_aRefusalFromTheServiceIsShown() {
        roster.getSettlers().add(settler("abc00000-0000-0000-0000-000000000000", "Aldric Tanner"));
        when(service.remove(eq(SITE), any(), any())).thenReturn(SettlerResult.refused("core.settler.not_loaded"));

        new SettlerDismissSubCommand(commands, service).execute(player, null, "Wolves", "abc");

        assertTrue(told(player, "core.settler.not_loaded"));
    }

    @Test
    void ac32_listAndDismissCompleteClanNames() {
        final CommandSender sender = mock(CommandSender.class);
        when(commands.clanNames("Wo")).thenReturn(List.of("Wolves"));

        assertEquals(List.of("Wolves"), new SettlerListSubCommand(commands, service).processTabComplete(sender, new String[]{"Wo"}));
        assertEquals(List.of("Wolves"), new SettlerDismissSubCommand(commands, service).processTabComplete(sender, new String[]{"Wo"}));
        assertTrue(new SettlerDismissSubCommand(commands, service).processTabComplete(sender, new String[]{"Wolves", ""}).isEmpty());
    }

    @Test
    void ac32_clanNamesMatchTheStartIgnoringCase() {
        final ClanManager clans = mock(ClanManager.class);
        final Clan bears = mock(Clan.class);
        when(bears.getName()).thenReturn("Bears");
        when(clans.getObjects()).thenReturn(Map.of(1L, clan, 2L, bears));
        final SettlerCommands real = MenuProbe.build(SettlerCommands.class, clans);

        assertEquals(List.of("Wolves"), real.clanNames("wo"));
    }
}
