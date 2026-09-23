package me.mykindos.betterpvp.core.world.construction.blueprint;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentSerializationRegistry;
import me.mykindos.betterpvp.core.item.renderer.LoreComponentRenderer;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.StructureType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

/**
 * A plan for one structure. Holding it shows where the structure would go, and using it builds it there.
 *
 * @see BlueprintSessions for how it is held and used
 */
@Singleton
@ItemKey("core:structure_blueprint")
public class StructureBlueprintItem extends BaseItem {

    @Inject
    private StructureBlueprintItem(@NotNull StructureCatalogue catalogue,
                                   @NotNull ComponentSerializationRegistry serializers) {
        super(Item.model("blueprint"), ItemGroup.MISC, item -> ItemRarity.COMMON, new LoreComponentRenderer(),
                item -> name(catalogue, item));
        serializers.register(new StructureBlueprintSerializer());
        addSerializableComponent(new StructureBlueprintComponent(""));
    }

    private static @NotNull Component name(@NotNull StructureCatalogue catalogue, @NotNull ItemInstance item) {
        final Component structure = item.getComponent(StructureBlueprintComponent.class)
                .flatMap(component -> catalogue.find(component.getStructure()))
                .map(StructureType::getDisplayName)
                .orElse(Translations.component("core.item.structure_blueprint.unknown"));
        return Translations.component("core.item.structure_blueprint.name", structure).color(TextColor.color(60, 125, 222));
    }
}
