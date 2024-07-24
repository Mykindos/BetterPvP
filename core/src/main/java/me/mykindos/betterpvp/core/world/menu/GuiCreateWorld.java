package me.mykindos.betterpvp.core.world.menu;

import lombok.Getter;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.stats.menu.CycleButton;
import me.mykindos.betterpvp.core.world.WorldHandler;
import me.mykindos.betterpvp.core.world.menu.button.CreateButton;
import me.mykindos.betterpvp.core.world.menu.button.HardcoreButton;
import me.mykindos.betterpvp.core.world.menu.button.KeepSpawnLoadedButton;
import me.mykindos.betterpvp.core.world.menu.button.SetSeedButton;
import me.mykindos.betterpvp.core.world.menu.button.StructuresButton;
import me.mykindos.betterpvp.core.world.menu.button.VoidButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class GuiCreateWorld extends AbstractGui implements Windowed {

    private static final WorldType[] TYPES = new WorldType[] { WorldType.FLAT, WorldType.NORMAL, WorldType.LARGE_BIOMES, WorldType.AMPLIFIED };

    private final WorldCreator creator;

    public GuiCreateWorld(WorldHandler worldHandler, String worldName, @Nullable Windowed previous) {
        super(9, 5);
        this.creator = new WorldCreator(worldName);

        setItem(10, new SetSeedButton());
        setItem(12, new VoidButton());
        setItem(14, new CycleButton<>(World.Environment.values(), type -> switch (type) {
            case NORMAL -> Material.GRASS_BLOCK;
            case NETHER -> Material.NETHERRACK;
            case THE_END -> Material.END_STONE;
            case CUSTOM -> Material.GLASS;
        }, type -> Component.text("Dimension: ", NamedTextColor.GRAY).append(switch (type) {
            case NORMAL -> Component.text("Normal", TextColor.color(82, 199, 85));
            case NETHER -> Component.text("Nether", TextColor.color(214, 92, 92));
            case THE_END -> Component.text("The End", TextColor.color(223, 224, 119));
            case CUSTOM -> Component.text("None", TextColor.color(82, 135, 209));
        }), creator::environment));
        setItem(16, new CycleButton<>(TYPES, type -> switch (type) {
            case FLAT -> Material.SMALL_AMETHYST_BUD;
            case NORMAL -> Material.MEDIUM_AMETHYST_BUD;
            case LARGE_BIOMES -> Material.LARGE_AMETHYST_BUD;
            case AMPLIFIED -> Material.AMETHYST_CLUSTER;
        }, type -> Component.text("World Type: ", NamedTextColor.GRAY).append(switch (type) {
            case FLAT -> Component.text("Flat", TextColor.color(147, 252, 129));
            case NORMAL -> Component.text("Normal", TextColor.color(240, 252, 131));
            case LARGE_BIOMES -> Component.text("Large Biomes", TextColor.color(245, 174, 113));
            case AMPLIFIED -> Component.text("Amplified", TextColor.color(247, 101, 82));
        }), creator::type));
        setItem(29, new StructuresButton());
        setItem(31, new HardcoreButton());
        setItem(33, new KeepSpawnLoadedButton());
        setItem(36, new BackButton(previous));
        setItem(44, new CreateButton(worldHandler, creator));
        setBackground(Menu.BACKGROUND_ITEM);
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("Create a World");
    }
}
