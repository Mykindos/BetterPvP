package me.mykindos.betterpvp.core.world.construction.blueprint;

import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.world.construction.ComponentKeys;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.TestStructureType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StructureBlueprintTest {

    private final StructureBlueprintSerializer serializer = new StructureBlueprintSerializer();
    private final Map<NamespacedKey, Object> stored = new HashMap<>();
    private final PersistentDataContainer container = container(stored);

    @Test
    void ac1_aBuildBlueprintKeepsItsStructureType() {
        serializer.serialize(new StructureBlueprintComponent("hall"), container);

        final StructureBlueprintComponent read = serializer.deserialize(mock(ItemInstance.class), container);

        assertEquals("hall", read.getStructure());
        assertNull(read.getMoving());
    }

    @Test
    void ac1_aMoveBlueprintKeepsItsStructureTypeAndThePlacedStructureItMoves() {
        final UUID moving = UUID.randomUUID();
        serializer.serialize(new StructureBlueprintComponent("hall", moving), container);

        final StructureBlueprintComponent read = serializer.deserialize(mock(ItemInstance.class), container);

        assertEquals("hall", read.getStructure());
        assertEquals(moving, read.getMoving());
    }

    @Test
    void ac1_writingABuildBlueprintOverAMoveBlueprintDropsTheMove() {
        serializer.serialize(new StructureBlueprintComponent("hall", UUID.randomUUID()), container);
        serializer.serialize(new StructureBlueprintComponent("hall"), container);

        assertNull(serializer.deserialize(mock(ItemInstance.class), container).getMoving());
    }

    @Test
    void ac1_aMoveIdThatIsNotAUuidReadsBackAsAPlainBlueprint() {
        final UUID moving = UUID.randomUUID();
        serializer.serialize(new StructureBlueprintComponent("hall", moving), container);
        final NamespacedKey movingKey = stored.entrySet().stream()
                .filter(entry -> moving.toString().equals(entry.getValue()))
                .map(Map.Entry::getKey).findFirst().orElseThrow();
        serializer.serialize(new StructureBlueprintComponent("hall"), container);
        stored.put(movingKey, "not-a-uuid");

        final StructureBlueprintComponent read = serializer.deserialize(mock(ItemInstance.class), container);

        assertEquals("hall", read.getStructure());
        assertNull(read.getMoving());
    }

    @Test
    void ac3_aBuildBlueprintIsNamedAfterItsStructure() {
        final StructureCatalogue catalogue = catalogue();

        final Component name = StructureBlueprintItem.name(catalogue, item(new StructureBlueprintComponent("hall")));

        final TranslatableComponent found = ComponentKeys.find(name, "core.item.structure_blueprint.name").orElseThrow();
        assertEquals("Hall", ComponentKeys.text(found));
    }

    @Test
    void ac3_aMoveBlueprintIsNamedAsAMoveOfItsStructure() {
        final StructureCatalogue catalogue = catalogue();

        final Component name = StructureBlueprintItem.name(catalogue,
                item(new StructureBlueprintComponent("hall", UUID.randomUUID())));

        final TranslatableComponent found = ComponentKeys.find(name, "core.item.structure_blueprint.move_name")
                .orElseThrow();
        assertEquals("Hall", ComponentKeys.text(found));
    }

    @Test
    void ac3_aBlueprintOfAnUnknownTypeGetsTheUnknownName() {
        final StructureCatalogue catalogue = catalogue();

        final Component name = StructureBlueprintItem.name(catalogue, item(new StructureBlueprintComponent("ruin")));

        assertTrue(ComponentKeys.hasKey(name, "core.item.structure_blueprint.unknown"));
    }

    private static @NotNull ItemInstance item(@NotNull StructureBlueprintComponent component) {
        final ItemInstance item = mock(ItemInstance.class);
        when(item.getComponent(StructureBlueprintComponent.class)).thenReturn(Optional.of(component));
        return item;
    }

    private static @NotNull StructureCatalogue catalogue() {
        final StructureCatalogue catalogue = new StructureCatalogue();
        catalogue.register(new TestStructureType("hall"));
        return catalogue;
    }

    @SuppressWarnings("unchecked")
    private static @NotNull PersistentDataContainer container(@NotNull Map<NamespacedKey, Object> stored) {
        final PersistentDataContainer container = mock(PersistentDataContainer.class);
        doAnswer(invocation -> stored.put(invocation.getArgument(0), invocation.getArgument(2)))
                .when(container).set(any(NamespacedKey.class), any(), any());
        doAnswer(invocation -> stored.remove(invocation.<NamespacedKey>getArgument(0)))
                .when(container).remove(any(NamespacedKey.class));
        when(container.get(any(NamespacedKey.class), any())).thenAnswer(invocation -> stored.get(invocation.getArgument(0)));
        return container;
    }
}
