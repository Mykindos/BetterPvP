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
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.minecraft.MinecraftCraftingRecipeAdapter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Namespaced;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;

import java.util.Objects;

@Singleton
public class CoreItemBootstrap {

    private final Core core;
    private final ItemRegistry itemRegistry;
    private final RuneRegistry runeRegistry;
    private final CraftingRecipeRegistry craftingRegistry;
    private final MinecraftCraftingRecipeAdapter adapter;

    @Inject
    private CoreItemBootstrap(Core core, ItemRegistry itemRegistry, RuneRegistry runeRegistry, CraftingRecipeRegistry craftingRegistry, MinecraftCraftingRecipeAdapter adapter) {
        this.core = core;
        this.itemRegistry = itemRegistry;
        this.runeRegistry = runeRegistry;
        this.craftingRegistry = craftingRegistry;
        this.adapter = adapter;
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
    private void registerRare(MagicSeal magicSeal, Blackroot blackroot, RazorEdge razorEdge) {
        itemRegistry.registerItem(key("magic_seal"), magicSeal);
        itemRegistry.registerItem(key("blackroot"), blackroot);
        itemRegistry.registerItem(key("razor_edge"), razorEdge);
    }

    @Inject
    private void registerEpic(Duskhide duskhide, FeatherOfZephyr featherOfZephyr, PolariteChunk polariteChunk, MagneticShard magneticShard,
                              ColossusFragment colossusFragment, OverchargedCrystal overchargedCrystal, AlligatorScale alligatorScale,
                              FangOfTheDeep fangOfTheDeep, VolticShield volticShield, StormsteelPlate stormsteelPlate) {
        itemRegistry.registerItem(key("divine_amulet"), new BaseItem("Divine Amulet", Item.model("divine_amulet", 16), ItemGroup.MATERIAL, ItemRarity.EPIC));
        itemRegistry.registerItem(key("emerald_talisman"), new BaseItem("Emerald Talisman", Item.model("emerald_talisman", 16), ItemGroup.MATERIAL, ItemRarity.EPIC));
        itemRegistry.registerItem(key("duskhide"), duskhide);
        itemRegistry.registerItem(key("feather_of_zephyr"), featherOfZephyr);
        itemRegistry.registerItem(key("polarite_chunk"), polariteChunk);
        itemRegistry.registerItem(key("magnetic_shard"), magneticShard);
        itemRegistry.registerItem(key("colossus_fragment"), colossusFragment);
        itemRegistry.registerItem(key("overcharged_crystal"), overchargedCrystal);
        itemRegistry.registerItem(key("alligator_scale"), alligatorScale);
        itemRegistry.registerItem(key("fang_of_the_deep"), fangOfTheDeep);
        itemRegistry.registerItem(key("voltic_shield"), volticShield);
        itemRegistry.registerItem(key("stormsteel_plate"), stormsteelPlate);
    }

    @Inject
    private void registerLegendary(DurakHandle durakHandle, AetherCore aetherCore, MeridianOrb meridianOrb, BurntRemnant burntRemnant,
                                   ReapersEdge reapersEdge) {
        itemRegistry.registerItem(key("phoenix_egg"), new BaseItem("Phoenix Egg", Item.model("phoenix_egg", 16), ItemGroup.MATERIAL, ItemRarity.LEGENDARY));
        itemRegistry.registerItem(key("durak_handle"), durakHandle);
        itemRegistry.registerItem(key("aether_core"), aetherCore);
        itemRegistry.registerItem(key("meridian_orb"), meridianOrb);
        itemRegistry.registerItem(key("burnt_remnant"), burntRemnant);
        itemRegistry.registerItem(key("reapers_edge"), reapersEdge);
    }

    @Inject
    private void registerMythical(StormInABottle stormInABottle, VoidglassCore voidglassCore) {
        itemRegistry.registerItem(key("storm_in_a_bottle"), stormInABottle);
        itemRegistry.registerItem(key("voidglass_core"), voidglassCore);
    }

    @Inject
    private void registerFuels(CoalItem coalItem, CharcoalItem charcoalItem) {
        registerFallbackItem("coal", Material.COAL, coalItem, true);
        registerFallbackItem("charcoal", Material.CHARCOAL, charcoalItem, true);
    }

    private void registerFallbackItem(String key, Material material, BaseItem item, boolean keepRecipe) {
        itemRegistry.registerFallbackItem(new NamespacedKey("minecraft", key), material, item);
        if (keepRecipe) {
            final Recipe old = Bukkit.getRecipe(material.getKey());
            if (old == null) return;
            final CraftingRecipe craftingRecipe = adapter.convertToCustomRecipe(old);
            if (craftingRecipe != null) craftingRegistry.registerRecipe(craftingRecipe);
        }
    }
}
