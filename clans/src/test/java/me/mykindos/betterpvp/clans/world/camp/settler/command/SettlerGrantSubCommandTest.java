package me.mykindos.betterpvp.clans.world.camp.settler.command;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerResult;
import me.mykindos.betterpvp.core.world.settler.SettlerTemplate;
import me.mykindos.betterpvp.core.world.settler.recruit.SettlerGrants;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
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
}
