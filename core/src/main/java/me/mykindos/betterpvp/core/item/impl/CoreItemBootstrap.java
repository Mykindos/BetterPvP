package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.component.impl.blueprint.BlueprintItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneRegistry;
import me.mykindos.betterpvp.core.item.component.impl.runes.scorching.ScorchingRune;
import me.mykindos.betterpvp.core.item.component.impl.runes.scorching.ScorchingRuneItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.unbreaking.UnbreakingRune;
import me.mykindos.betterpvp.core.item.component.impl.runes.unbreaking.UnbreakingRuneItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

@Singleton
public class CoreItemBootstrap {

    private final Core core;
    private final ItemRegistry itemRegistry;
    private final RuneRegistry runeRegistry;

    @Inject
    private CoreItemBootstrap(Core core, ItemRegistry itemRegistry, RuneRegistry runeRegistry) {
        this.core = core;
        this.itemRegistry = itemRegistry;
        this.runeRegistry = runeRegistry;
    }

    private NamespacedKey key(String name) {
        return new NamespacedKey(core, name);
    }

    @Inject
    private void registerRunes(UnbreakingRuneItem unbreakingRuneItem, UnbreakingRune unbreakingRune,
                               ScorchingRuneItem scorchingRuneItem, ScorchingRune scorchingRune) {
        itemRegistry.registerItem(key("unbreaking_rune"), unbreakingRuneItem);
        runeRegistry.registerRune(unbreakingRune);
        itemRegistry.registerItem(key("scorching_rune"), scorchingRuneItem);
        runeRegistry.registerRune(scorchingRune);
    }

    // Registered items that need to have an order in which they are registered
    // Or are not tied to a specific rarity
    @Inject
    private void registerItems(BlueprintItem blueprintItem) {
        itemRegistry.registerItem(key( "blueprint"), blueprintItem);
    }

    @Inject
    private void registerCommon(Hammer hammer, Rope rope) {
        itemRegistry.registerItem(key("cloth"), new BaseItem("Cloth", Item.model("cloth", 64), ItemGroup.MATERIAL, ItemRarity.COMMON));
        itemRegistry.registerItem(key("cut_stone"), new BaseItem("Cut Stone", Item.model("cut_stone", 64), ItemGroup.MATERIAL, ItemRarity.COMMON));
        itemRegistry.registerItem(key("torn_cloth"), new BaseItem("Torn Cloth", Item.model("torn_cloth", 64), ItemGroup.MATERIAL, ItemRarity.COMMON));
        itemRegistry.registerItem(key("stone"), new BaseItem("Stone", Item.model("stone", 64), ItemGroup.MATERIAL, ItemRarity.COMMON));
        itemRegistry.registerItem(key("hammer"), hammer);
        itemRegistry.registerItem(key("rope"), rope);
    }

    @Inject
    private void registerUncommon() {
        itemRegistry.registerItem(key("toxic_gem"), new BaseItem("Toxic Gem", Item.model("toxic_gem", 16), ItemGroup.MATERIAL, ItemRarity.UNCOMMON));
    }

    @Inject
    private void registerRare(MagicSeal magicSeal, Blackroot blackroot) {
        itemRegistry.registerItem(key("magic_seal"), magicSeal);
        itemRegistry.registerItem(key("blackroot"), blackroot);
    }

    @Inject
    private void registerEpic(Duskhide duskhide) {
        itemRegistry.registerItem(key("divine_amulet"), new BaseItem("Divine Amulet", Item.model("divine_amulet", 16), ItemGroup.MATERIAL, ItemRarity.EPIC));
        itemRegistry.registerItem(key("emerald_talisman"), new BaseItem("Emerald Talisman", Item.model("emerald_talisman", 16), ItemGroup.MATERIAL, ItemRarity.EPIC));
        itemRegistry.registerItem(key("duskhide"), duskhide);
    }

    @Inject
    private void registerLegendary(DurakHandle durakHandle) {
        itemRegistry.registerItem(key("phoenix_egg"), new BaseItem("Phoenix Egg", Item.model("phoenix_egg", 16), ItemGroup.MATERIAL, ItemRarity.LEGENDARY));
        itemRegistry.registerItem(key("durak_handle"), durakHandle);
    }

    @Inject
    private void registerMythical(StormInABottle stormInABottle, VoidglassCore voidglassCore) {
        itemRegistry.registerItem(key("storm_in_a_bottle"), stormInABottle);
        itemRegistry.registerItem(key("voidglass_core"), voidglassCore);
    }

    @Inject
    private void registerFuels(CoalItem coalItem, CharcoalItem charcoalItem) {
        itemRegistry.registerFallbackItem(key("coal"), Material.COAL, coalItem);
        itemRegistry.registerFallbackItem(key("charcoal"), Material.CHARCOAL, charcoalItem);
    }
}
