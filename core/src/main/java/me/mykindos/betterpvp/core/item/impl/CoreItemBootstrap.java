package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemBootstrap;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.component.impl.blueprint.BlueprintItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneRegistry;
import me.mykindos.betterpvp.core.item.component.impl.runes.scorching.ScorchingRune;
import me.mykindos.betterpvp.core.item.component.impl.runes.scorching.ScorchingRuneItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.unbreaking.UnbreakingRune;
import me.mykindos.betterpvp.core.item.component.impl.runes.unbreaking.UnbreakingRuneItem;
import me.mykindos.betterpvp.core.item.impl.cannon.CannonItem;
import me.mykindos.betterpvp.core.item.impl.cannon.CannonballItem;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.minecraft.MinecraftCraftingRecipeAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;

@Singleton
public class CoreItemBootstrap implements ItemBootstrap {

    private boolean registered = false;

    @Inject private ItemRegistry itemRegistry;
    @Inject private Core core;
    @Inject private RuneRegistry runeRegistry;
    @Inject private CraftingRecipeRegistry craftingRegistry;
    @Inject private MinecraftCraftingRecipeAdapter adapter;

    // Item dependencies
    @Inject private UnbreakingRuneItem unbreakingRuneItem;
    @Inject private UnbreakingRune unbreakingRune;
    @Inject private ScorchingRuneItem scorchingRuneItem;
    @Inject private ScorchingRune scorchingRune;
    @Inject private BlueprintItem blueprintItem;
    @Inject private Hammer hammer;
    @Inject private Rope rope;
    @Inject private MagicSeal magicSeal;
    @Inject private Blackroot blackroot;
    @Inject private RazorEdge razorEdge;
    @Inject private Duskhide duskhide;
    @Inject private FeatherOfZephyr featherOfZephyr;
    @Inject private PolariteChunk polariteChunk;
    @Inject private MagneticShard magneticShard;
    @Inject private ColossusFragment colossusFragment;
    @Inject private OverchargedCrystal overchargedCrystal;
    @Inject private AlligatorScale alligatorScale;
    @Inject private FangOfTheDeep fangOfTheDeep;
    @Inject private VolticShield volticShield;
    @Inject private StormsteelPlate stormsteelPlate;
    @Inject private DurakHandle durakHandle;
    @Inject private AetherCore aetherCore;
    @Inject private MeridianOrb meridianOrb;
    @Inject private BurntRemnant burntRemnant;
    @Inject private ReapersEdge reapersEdge;
    @Inject private StormInABottle stormInABottle;
    @Inject private VoidglassCore voidglassCore;
    @Inject private CoalItem coalItem;
    @Inject private CharcoalItem charcoalItem;
    @Inject private CannonItem cannonItem;
    @Inject private CannonballItem cannonballItem;

    private NamespacedKey key(String name) {
        return new NamespacedKey(core, name);
    }

    @Inject
    @Override
    public void registerItems() {
        if (registered) return;
        registered = true;

        // Runes
        itemRegistry.registerItem(key("unbreaking_rune"), unbreakingRuneItem);
        runeRegistry.registerRune(unbreakingRune);
        itemRegistry.registerItem(key("scorching_rune"), scorchingRuneItem);
        runeRegistry.registerRune(scorchingRune);

        // Misc items
        itemRegistry.registerItem(key("blueprint"), blueprintItem);
        itemRegistry.registerItem(key("cannon"), cannonItem);
        itemRegistry.registerItem(key("cannonball"), cannonballItem);

        // Common
        itemRegistry.registerItem(key("cloth"), new BaseItem("Cloth", Item.model("cloth", 64), ItemGroup.MATERIAL, ItemRarity.COMMON));
        itemRegistry.registerItem(key("cut_stone"), new BaseItem("Cut Stone", Item.model("cut_stone", 64), ItemGroup.MATERIAL, ItemRarity.COMMON));
        itemRegistry.registerItem(key("torn_cloth"), new BaseItem("Torn Cloth", Item.model("torn_cloth", 64), ItemGroup.MATERIAL, ItemRarity.COMMON));
        itemRegistry.registerItem(key("stone"), new BaseItem("Stone", Item.model("stone", 64), ItemGroup.MATERIAL, ItemRarity.COMMON));
        itemRegistry.registerItem(key("hammer"), hammer);
        itemRegistry.registerItem(key("rope"), rope);

        // Uncommon
        itemRegistry.registerItem(key("toxic_gem"), new BaseItem("Toxic Gem", Item.model("toxic_gem", 16), ItemGroup.MATERIAL, ItemRarity.UNCOMMON));

        // Rare
        itemRegistry.registerItem(key("magic_seal"), magicSeal);
        itemRegistry.registerItem(key("blackroot"), blackroot);
        itemRegistry.registerItem(key("razor_edge"), razorEdge);

        // Epic
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

        // Legendary
        itemRegistry.registerItem(key("phoenix_egg"), new BaseItem("Phoenix Egg", Item.model("phoenix_egg", 16), ItemGroup.MATERIAL, ItemRarity.LEGENDARY));
        itemRegistry.registerItem(key("durak_handle"), durakHandle);
        itemRegistry.registerItem(key("aether_core"), aetherCore);
        itemRegistry.registerItem(key("meridian_orb"), meridianOrb);
        itemRegistry.registerItem(key("burnt_remnant"), burntRemnant);
        itemRegistry.registerItem(key("reapers_edge"), reapersEdge);

        // Mythical
        itemRegistry.registerItem(key("storm_in_a_bottle"), stormInABottle);
        itemRegistry.registerItem(key("voidglass_core"), voidglassCore);

        // Fuels
        registerFallbackItem(itemRegistry, "coal", Material.COAL, coalItem, true);
        registerFallbackItem(itemRegistry,"charcoal", Material.CHARCOAL, charcoalItem, true);
    }

    private void registerFallbackItem(ItemRegistry itemRegistry, String key, Material material, BaseItem item, boolean keepRecipe) {
        final NamespacedKey namespacedKey = new NamespacedKey("minecraft", key);
        itemRegistry.registerFallbackItem(namespacedKey, material, item);
        if (keepRecipe) {
            final Recipe old = Bukkit.getRecipe(material.getKey());
            if (old == null) return;
            final CraftingRecipe craftingRecipe = adapter.convertToCustomRecipe(old);
            if (craftingRecipe != null) craftingRegistry.registerRecipe(namespacedKey, craftingRecipe);
        }
    }
}
