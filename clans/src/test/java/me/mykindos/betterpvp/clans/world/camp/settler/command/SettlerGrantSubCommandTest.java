package me.mykindos.betterpvp.clans.world.camp.settler.command;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerResult;
import me.mykindos.betterpvp.core.world.settler.SettlerTemplate;
import me.mykindos.betterpvp.core.world.settler.recruit.SettlerGrants;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static me.mykindos.betterpvp.clans.world.camp.settler.menu.MenuProbe.mentions;
import static me.mykindos.betterpvp.clans.world.camp.settler.menu.MenuProbe.text;
import static me.mykindos.betterpvp.clans.world.camp.settler.menu.MenuProbe.told;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SettlerGrantSubCommandTest {

    private static final long CLAN = 42;

    private final SettlerCommands commands = mock(SettlerCommands.class);
    private final SettlerGrants grants = mock(SettlerGrants.class);
    private final Player player = mock(Player.class);
    private final Clan clan = mock(Clan.class);

    @BeforeEach
    void setUp() {
        when(clan.getId()).thenReturn(CLAN);
        when(clan.getName()).thenReturn("Wolves");
        when(commands.professionIds()).thenReturn(List.of("builder", "farmer"));
        when(commands.line(any())).thenReturn(Component.empty());
        doAnswer(invocation -> {
            invocation.<Consumer<Clan>>getArgument(2).accept(clan);
            return null;
        }).when(commands).withCamp(eq(player), eq("Wolves"), any());

        final Settler settler = new Settler();
        settler.setId(UUID.randomUUID());
        settler.setName("Tamsin Reed");
        settler.setRarity(SettlerRarity.RARE);
        when(grants.grant(any(), any())).thenReturn(SettlerResult.done(settler));
    }

    private SettlerGrantSubCommand command() {
        return new SettlerGrantSubCommand(commands, grants);
    }

    private List<Component> sent() {
        final ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        verify(player, atLeast(1)).sendMessage(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void ac28_staffGrantsGoThroughSettlerGrants() {
        final SettlerGrantSubCommand command = new SettlerGrantSubCommand(commands, grants);

        command.execute(player, null, "Wolves", "rare", "builder", "dungeon", "the", "Sunken", "Keep");

        verify(grants).grant(Camps.keyFor(CLAN), SettlerTemplate.builder()
                .rarity(SettlerRarity.RARE)
                .profession("builder")
                .source("dungeon")
                .historyArg("the Sunken Keep")
                .build());
    }

    @Test
    void ac28_anUnknownProfessionNeverReachesTheGrant() {
        final SettlerGrantSubCommand command = new SettlerGrantSubCommand(commands, grants);

        command.execute(player, null, "Wolves", "rare", "ghost");

        verify(grants, never()).grant(any(), any());
    }

    @Test
    void ac31_theSourceDefaultsToHiringAndNoneLeavesNoProfession() {
        command().execute(player, null, "Wolves", "common", "none");

        verify(grants).grant(Camps.keyFor(CLAN), SettlerTemplate.builder()
                .rarity(SettlerRarity.COMMON)
                .source("hiring")
                .build());
    }

    @Test
    void ac31_aGrantSaysWhoWasAddedAndShowsItsLine() {
        when(commands.line(any())).thenReturn(Component.text("line Tamsin Reed"));

        command().execute(player, null, "Wolves", "rare");

        assertTrue(told(player, "clans.command.settler.granted"));
        final String granted = text(sent().getFirst());
        assertTrue(granted.contains("Tamsin Reed") && granted.contains("Wolves"));
        assertTrue(text(sent().get(1)).contains("line Tamsin Reed"));
        assertTrue(sent().stream().allMatch(message -> mentions(message, "clans.prefix.settler")));
    }

    @Test
    void ac31_anUnknownRarityIsRefusedByNameAndNeverReachesTheGrant() {
        command().execute(player, null, "Wolves", "mythic", "builder");

        assertTrue(told(player, "clans.command.settler.unknown_rarity"));
        assertTrue(text(sent().getFirst()).contains("mythic"));
        verify(grants, never()).grant(any(), any());
        verify(commands, never()).withCamp(any(), any(), any());
    }

    @Test
    void ac31_anUnknownProfessionIsRefusedByName() {
        command().execute(player, null, "Wolves", "rare", "ghost");

        assertTrue(told(player, "clans.command.settler.unknown_profession"));
        assertTrue(text(sent().getFirst()).contains("ghost"));
        verify(commands, never()).withCamp(any(), any(), any());
    }

    @Test
    void ac31_aRefusalFromTheGrantIsShown() {
        when(grants.grant(any(), any())).thenReturn(SettlerResult.refused("core.settler.population_full"));

        command().execute(player, null, "Wolves", "rare");

        assertTrue(told(player, "core.settler.population_full"));
        assertTrue(sent().stream().noneMatch(message -> mentions(message, "clans.command.settler.granted")));
    }

    @Test
    void ac26_grantWithoutARarityShowsItsUsage() {
        command().execute(player, null, "Wolves");

        assertTrue(told(player, "clans.command.settler.grant.usage"));
        verify(grants, never()).grant(any(), any());
    }

    @Test
    void ac32_grantCompletesClanRarityProfessionThenSource() {
        final CommandSender sender = mock(CommandSender.class);
        when(commands.clanNames("wo")).thenReturn(List.of("Wolves"));
        when(commands.historySources()).thenReturn(List.of("hiring", "dungeon"));

        assertEquals(List.of("Wolves"), command().processTabComplete(sender, new String[]{"Wo"}));
        assertEquals(List.of("common", "uncommon", "rare", "legendary"),
                command().processTabComplete(sender, new String[]{"Wolves", ""}));
        assertEquals(List.of("rare"), command().processTabComplete(sender, new String[]{"Wolves", "ra"}));
        assertEquals(List.of("builder", "farmer", "none"),
                command().processTabComplete(sender, new String[]{"Wolves", "rare", ""}));
        assertEquals(List.of("dungeon"),
                command().processTabComplete(sender, new String[]{"Wolves", "rare", "builder", "d"}));
        assertTrue(command().processTabComplete(sender, new String[]{"Wolves", "rare", "builder", "dungeon", ""}).isEmpty());
    }
}
