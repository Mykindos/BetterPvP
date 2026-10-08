package me.mykindos.betterpvp.core.world.schematic.command;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.world.construction.ComponentKeys;
import me.mykindos.betterpvp.core.world.construction.ConstructionResult;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructurePosition;
import me.mykindos.betterpvp.core.world.construction.TestStructureType;
import me.mykindos.betterpvp.core.world.construction.view.StructureViews;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StructureDisableSubCommandTest {

    private final StructureViews views = mock(StructureViews.class);
    private final ConstructionService construction = mock(ConstructionService.class);
    private final StructureCatalogue catalogue = new StructureCatalogue();
    private final Player player = mock(Player.class);
    private final World world = mock(World.class);
    private final Block target = mock(Block.class);
    private final PlacedStructure hall = new PlacedStructure(UUID.randomUUID(), "hall",
            new StructurePosition(100, 64, 100, 0), StructureCondition.ACTIVE);

    private StructureDisableSubCommand command;

    @BeforeEach
    void setUp() {
        catalogue.register(new TestStructureType("hall"));
        when(player.getWorld()).thenReturn(world);
        when(target.getWorld()).thenReturn(world);
        when(player.getTargetBlockExact(anyInt())).thenReturn(target);
        when(views.structureAt(target)).thenReturn(Optional.of(hall));
        command = new StructureDisableSubCommand(views, construction, catalogue);
    }

    @Test
    void ac1_itDisablesTheStructureThePlayerIsLookingAtAndSaysSo() {
        when(construction.disable(world, hall.getId())).thenReturn(ConstructionResult.done(hall));

        command.execute(player, mock(Client.class));

        verify(construction).disable(world, hall.getId());
        final Component message = lastMessage();
        assertTrue(ComponentKeys.hasKey(message, "core.construction.command.disable.done"));
        assertTrue(ComponentKeys.text(message).contains("Hall"), "names the structure: " + ComponentKeys.text(message));
    }

    @Test
    void ac1_whenTheServiceRefusesThePlayerIsGivenItsReason() {
        when(construction.disable(world, hall.getId()))
                .thenReturn(ConstructionResult.refused("core.construction.disable_needs_active"));

        command.execute(player, mock(Client.class));

        final Component message = lastMessage();
        assertTrue(ComponentKeys.hasKey(message, "core.construction.disable_needs_active"));
        assertFalse(ComponentKeys.hasKey(message, "core.construction.command.disable.done"));
    }

    @Test
    void ac1_lookingAtNoStructureDisablesNothing() {
        when(views.structureAt(target)).thenReturn(Optional.empty());

        command.execute(player, mock(Client.class));

        verify(construction, never()).disable(any(), any());
        assertTrue(ComponentKeys.hasKey(lastMessage(), "core.construction.command.disable.no_structure"));
    }

    @Test
    void ac1_lookingAtNothingDisablesNothing() {
        when(player.getTargetBlockExact(anyInt())).thenReturn(null);

        command.execute(player, mock(Client.class));

        verify(construction, never()).disable(any(), any());
        assertTrue(ComponentKeys.hasKey(lastMessage(), "core.construction.command.disable.no_structure"));
    }

    private @NotNull Component lastMessage() {
        final ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        verify(player).sendMessage(captor.capture());
        return captor.getValue();
    }
}
