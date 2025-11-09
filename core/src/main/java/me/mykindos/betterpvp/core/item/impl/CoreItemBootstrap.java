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
import me.mykindos.betterpvp.core.item.component.impl.runes.attraction.AttractionRune;
import me.mykindos.betterpvp.core.item.component.impl.runes.attraction.AttractionRuneItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.brutality.BrutalityRune;
import me.mykindos.betterpvp.core.item.component.impl.runes.brutality.BrutalityRuneItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.detonation.DetonationRune;
import me.mykindos.betterpvp.core.item.component.impl.runes.detonation.DetonationRuneItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.essence.EssenceRune;
import me.mykindos.betterpvp.core.item.component.impl.runes.essence.EssenceRuneItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.ferocity.FerocityRune;
import me.mykindos.betterpvp.core.item.component.impl.runes.ferocity.FerocityRuneItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.flameguard.FlameguardRune;
import me.mykindos.betterpvp.core.item.component.impl.runes.flameguard.FlameguardRuneItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.forestwright.ForestwrightRune;
import me.mykindos.betterpvp.core.item.component.impl.runes.forestwright.ForestwrightRuneItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.greed.GreedRune;
import me.mykindos.betterpvp.core.item.component.impl.runes.greed.GreedRuneItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.hookmaster.HookmasterRune;
import me.mykindos.betterpvp.core.item.component.impl.runes.hookmaster.HookmasterRuneItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.momentum.MomentumRune;
import me.mykindos.betterpvp.core.item.component.impl.runes.momentum.MomentumRuneItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.moonseer.MoonseerRune;
import me.mykindos.betterpvp.core.item.component.impl.runes.moonseer.MoonseerRuneItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.recovery.RecoveryRune;
import me.mykindos.betterpvp.core.item.component.impl.runes.recovery.RecoveryRuneItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.scorching.ScorchingRune;
import me.mykindos.betterpvp.core.item.component.impl.runes.scorching.ScorchingRuneItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.slayer.SlayerRune;
import me.mykindos.betterpvp.core.item.component.impl.runes.slayer.SlayerRuneItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.stonecaller.StonecallerRune;
import me.mykindos.betterpvp.core.item.component.impl.runes.stonecaller.StonecallerRuneItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.unbreaking.UnbreakingRune;
import me.mykindos.betterpvp.core.item.component.impl.runes.unbreaking.UnbreakingRuneItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.vampirism.VampirismRune;
import me.mykindos.betterpvp.core.item.component.impl.runes.vampirism.VampirismRuneItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.wanderer.WandererRune;
import me.mykindos.betterpvp.core.item.component.impl.runes.wanderer.WandererRuneItem;
import me.mykindos.betterpvp.core.item.impl.cannon.CannonItem;
import me.mykindos.betterpvp.core.item.impl.cannon.CannonballItem;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.minecraft.MinecraftCraftingRecipeAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.List;
import java.util.Map;

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
    @Inject private FlameguardRuneItem flameguardRuneItem;
    @Inject private FlameguardRune flameguardRune;
    @Inject private AttractionRuneItem attractionRuneItem;
    @Inject private AttractionRune attractionRune;
    @Inject private RecoveryRuneItem recoveryRuneItem;
    @Inject private RecoveryRune recoveryRune;
    @Inject private WandererRuneItem wandererRuneItem;
    @Inject private WandererRune wandererRune;
    @Inject private MoonseerRuneItem moonseerRuneItem;
    @Inject private MoonseerRune moonseerRune;
    @Inject private ForestwrightRuneItem forestwrightRuneItem;
    @Inject private ForestwrightRune forestwrightRune;
    @Inject private HookmasterRuneItem hookmasterRuneItem;
    @Inject private HookmasterRune hookmasterRune;
    @Inject private StonecallerRuneItem stonecallerRuneItem;
    @Inject private StonecallerRune stonecallerRune;
    @Inject private FerocityRuneItem ferocityRuneItem;
    @Inject private FerocityRune ferocityRune;
    @Inject private DetonationRuneItem detonationRuneItem;
    @Inject private DetonationRune detonationRune;
    @Inject private GreedRuneItem greedRuneItem;
    @Inject private GreedRune greedRune;
    @Inject private VampirismRuneItem vampirismRuneItem;
    @Inject private VampirismRune vampirismRune;
    @Inject private SlayerRuneItem slayerRuneItem;
    @Inject private SlayerRune slayerRune;
    @Inject private BrutalityRuneItem brutalityRuneItem;
    @Inject private BrutalityRune brutalityRune;
    @Inject private MomentumRuneItem momentumRuneItem;
    @Inject private MomentumRune momentumRune;
    @Inject private EssenceRuneItem essenceRuneItem;
    @Inject private EssenceRune essenceRune;
    @Inject private BlueprintItem blueprintItem;
    @Inject private Hammer hammer;
    @Inject private Rope rope;
    @Inject private FishingRod fishingRod;
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
    @Inject private CutStone cutStone;
    @Inject private ShadowQuill shadowQuill;
    @Inject private EternalFlame eternalFlame;
    @Inject private RunicPlate runicPlate;
    @Inject private Cloth cloth;

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
        itemRegistry.registerItem(key("flameguard_rune"), flameguardRuneItem);
        runeRegistry.registerRune(flameguardRune);
        itemRegistry.registerItem(key("recovery_rune"), recoveryRuneItem);
        runeRegistry.registerRune(recoveryRune);
        itemRegistry.registerItem(key("attraction_rune"), attractionRuneItem);
        runeRegistry.registerRune(attractionRune);
        itemRegistry.registerItem(key("wanderer_rune"), wandererRuneItem);
        runeRegistry.registerRune(wandererRune);
        itemRegistry.registerItem(key("moonseer_rune"), moonseerRuneItem);
        runeRegistry.registerRune(moonseerRune);
        itemRegistry.registerItem(key("forestwright_rune"), forestwrightRuneItem);
        runeRegistry.registerRune(forestwrightRune);
        itemRegistry.registerItem(key("hookmaster_rune"), hookmasterRuneItem);
        runeRegistry.registerRune(hookmasterRune);
        itemRegistry.registerItem(key("stonecaller_rune"), stonecallerRuneItem);
        runeRegistry.registerRune(stonecallerRune);
        itemRegistry.registerItem(key("ferocity_rune"), ferocityRuneItem);
        runeRegistry.registerRune(ferocityRune);
        itemRegistry.registerItem(key("detonation_rune"), detonationRuneItem);
        runeRegistry.registerRune(detonationRune);
        itemRegistry.registerItem(key("greed_rune"), greedRuneItem);
        runeRegistry.registerRune(greedRune);
        itemRegistry.registerItem(key("vampirism_rune"), vampirismRuneItem);
        runeRegistry.registerRune(vampirismRune);
        itemRegistry.registerItem(key("slayer_rune"), slayerRuneItem);
        runeRegistry.registerRune(slayerRune);
        itemRegistry.registerItem(key("brutality_rune"), brutalityRuneItem);
        runeRegistry.registerRune(brutalityRune);
        itemRegistry.registerItem(key("momentum_rune"), momentumRuneItem);
        runeRegistry.registerRune(momentumRune);
        itemRegistry.registerItem(key("essence_rune"), essenceRuneItem);
        runeRegistry.registerRune(essenceRune);

        // Misc items
        itemRegistry.registerItem(key("blueprint"), blueprintItem);
        itemRegistry.registerItem(key("cannon"), cannonItem);
        itemRegistry.registerItem(key("cannonball"), cannonballItem);

        // Common
        itemRegistry.registerItem(key("cloth"), cloth);
        itemRegistry.registerItem(key("cut_stone"), cutStone);
        itemRegistry.registerItem(key("torn_cloth"), new BaseItem("Torn Cloth", Item.model("torn_cloth", 64), ItemGroup.MATERIAL, ItemRarity.COMMON));
        itemRegistry.registerItem(key("stone"), new BaseItem("Stone", Item.model("stone", 64), ItemGroup.MATERIAL, ItemRarity.COMMON));
        itemRegistry.registerItem(key("hammer"), hammer);
        itemRegistry.registerItem(key("rope"), rope);
        itemRegistry.registerItem(key("fishing_rod"), fishingRod);

        // Uncommon
        itemRegistry.registerItem(key("toxic_gem"), new BaseItem("Toxic Gem", Item.model("toxic_gem", 16), ItemGroup.MATERIAL, ItemRarity.UNCOMMON));
        itemRegistry.registerItem(key("runic_plate"), runicPlate);

        // Rare
        itemRegistry.registerItem(key("magic_seal"), magicSeal);
        itemRegistry.registerItem(key("blackroot"), blackroot);
        itemRegistry.registerItem(key("razor_edge"), razorEdge);
        itemRegistry.registerItem(key("shadow_quill"), shadowQuill);

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
        itemRegistry.registerItem(key("eternal_flame"), eternalFlame);

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

        // Tools - Swords
        registerFallbackItem(itemRegistry, "core:rustic_sword", Material.WOODEN_SWORD, new Sword("Rustic Sword", Material.WOODEN_SWORD, ItemRarity.COMMON), true);
        registerFallbackItem(itemRegistry, "core:crude_sword", Material.STONE_SWORD, new Sword("Crude Sword", Material.STONE_SWORD, ItemRarity.COMMON), true);
        registerFallbackItem(itemRegistry, "core:standard_sword", Material.IRON_SWORD, new Sword("Standard Sword", Material.IRON_SWORD, ItemRarity.COMMON), true);
        registerFallbackItem(itemRegistry, "core:booster_sword", Material.GOLDEN_SWORD, new Sword("Booster Sword", Material.GOLDEN_SWORD, ItemRarity.UNCOMMON), true);
        registerFallbackItem(itemRegistry, "core:power_sword", Material.DIAMOND_SWORD, new Sword("Power Sword", Material.DIAMOND_SWORD, ItemRarity.UNCOMMON), true);
        registerFallbackItem(itemRegistry, "core:ancient_sword", Material.NETHERITE_SWORD, new Sword("Ancient Sword", Material.NETHERITE_SWORD, ItemRarity.UNCOMMON), true);

        // Tools - Axes
        registerFallbackItem(itemRegistry, "core:rustic_axe", Material.WOODEN_AXE, new Axe("Rustic Axe", Material.WOODEN_AXE, ItemRarity.COMMON), true);
        registerFallbackItem(itemRegistry, "core:crude_axe", Material.STONE_AXE, new Axe("Crude Axe", Material.STONE_AXE, ItemRarity.COMMON), true);
        registerFallbackItem(itemRegistry, "core:standard_axe", Material.IRON_AXE, new Axe("Standard Axe", Material.IRON_AXE, ItemRarity.COMMON), true);
        registerFallbackItem(itemRegistry, "core:booster_axe", Material.GOLDEN_AXE, new Axe("Booster Axe", Material.GOLDEN_AXE, ItemRarity.UNCOMMON), true);
        registerFallbackItem(itemRegistry, "core:power_axe", Material.DIAMOND_AXE, new Axe("Power Axe", Material.DIAMOND_AXE, ItemRarity.UNCOMMON), true);
        registerFallbackItem(itemRegistry, "core:ancient_axe", Material.NETHERITE_AXE, new Axe("Ancient Axe", Material.NETHERITE_AXE, ItemRarity.UNCOMMON), true);

        // Tools - Pickaxes
        registerFallbackItem(itemRegistry, "core:rustic_pickaxe", Material.WOODEN_PICKAXE, new Pickaxe("Rustic Pickaxe", Material.WOODEN_PICKAXE, ItemRarity.COMMON), true);
        registerFallbackItem(itemRegistry, "core:crude_pickaxe", Material.STONE_PICKAXE, new Pickaxe("Crude Pickaxe", Material.STONE_PICKAXE, ItemRarity.COMMON), true);
        registerFallbackItem(itemRegistry, "core:standard_pickaxe", Material.IRON_PICKAXE, new Pickaxe("Standard Pickaxe", Material.IRON_PICKAXE, ItemRarity.COMMON), true);
        registerFallbackItem(itemRegistry, "core:booster_pickaxe", Material.GOLDEN_PICKAXE, new Pickaxe("Booster Pickaxe", Material.GOLDEN_PICKAXE, ItemRarity.UNCOMMON), true);
        registerFallbackItem(itemRegistry, "core:power_pickaxe", Material.DIAMOND_PICKAXE, new Pickaxe("Power Pickaxe", Material.DIAMOND_PICKAXE, ItemRarity.UNCOMMON), true);
        registerFallbackItem(itemRegistry, "core:ancient_pickaxe", Material.NETHERITE_PICKAXE, new Pickaxe("Ancient Pickaxe", Material.NETHERITE_PICKAXE, ItemRarity.UNCOMMON), true);

        // Tools - Shovels
        registerFallbackItem(itemRegistry, "core:rustic_shovel", Material.WOODEN_SHOVEL, new Shovel("Rustic Shovel", Material.WOODEN_SHOVEL, ItemRarity.COMMON), true);
        registerFallbackItem(itemRegistry, "core:crude_shovel", Material.STONE_SHOVEL, new Shovel("Crude Shovel", Material.STONE_SHOVEL, ItemRarity.COMMON), true);
        registerFallbackItem(itemRegistry, "core:standard_shovel", Material.IRON_SHOVEL, new Shovel("Standard Shovel", Material.IRON_SHOVEL, ItemRarity.COMMON), true);
        registerFallbackItem(itemRegistry, "core:booster_shovel", Material.GOLDEN_SHOVEL, new Shovel("Booster Shovel", Material.GOLDEN_SHOVEL, ItemRarity.UNCOMMON), true);
        registerFallbackItem(itemRegistry, "core:power_shovel", Material.DIAMOND_SHOVEL, new Shovel("Power Shovel", Material.DIAMOND_SHOVEL, ItemRarity.UNCOMMON), true);
        registerFallbackItem(itemRegistry, "core:ancient_shovel", Material.NETHERITE_SHOVEL, new Shovel("Ancient Shovel", Material.NETHERITE_SHOVEL, ItemRarity.UNCOMMON), true);

        // Tools - Hoes
        registerFallbackItem(itemRegistry, "core:rustic_hoe", Material.WOODEN_HOE, new Hoe("Rustic Hoe", Material.WOODEN_HOE, ItemRarity.COMMON), true);
        registerFallbackItem(itemRegistry, "core:crude_hoe", Material.STONE_HOE, new Hoe("Crude Hoe", Material.STONE_HOE, ItemRarity.COMMON), true);
        registerFallbackItem(itemRegistry, "core:standard_hoe", Material.IRON_HOE, new Hoe("Standard Hoe", Material.IRON_HOE, ItemRarity.COMMON), true);
        registerFallbackItem(itemRegistry, "core:booster_hoe", Material.GOLDEN_HOE, new Hoe("Booster Hoe", Material.GOLDEN_HOE, ItemRarity.UNCOMMON), true);
        registerFallbackItem(itemRegistry, "core:power_hoe", Material.DIAMOND_HOE, new Hoe("Power Hoe", Material.DIAMOND_HOE, ItemRarity.UNCOMMON), true);
        registerFallbackItem(itemRegistry, "core:ancient_hoe", Material.NETHERITE_HOE, new Hoe("Ancient Hoe", Material.NETHERITE_HOE, ItemRarity.UNCOMMON), true);

        // Tools - Bow
        registerFallbackItem(itemRegistry, "bow", Material.BOW, new Bow("Bow", Material.BOW, ItemRarity.COMMON), true);

        // Fuels
        registerFallbackItem(itemRegistry, "coal", Material.COAL, coalItem, true);
        registerFallbackItem(itemRegistry,"charcoal", Material.CHARCOAL, charcoalItem, true);
    }

    private void registerFallbackItem(ItemRegistry itemRegistry, String key, Material material, BaseItem item, boolean keepRecipe) {
        final NamespacedKey namespacedKey = NamespacedKey.fromString(key);
        itemRegistry.registerFallbackItem(namespacedKey, material, item);
        final List<Recipe> old = Bukkit.getRecipesFor(ItemStack.of(material));
        if (old.isEmpty()) {
            return;
        }

        final Map<NamespacedKey, CraftingRecipe> disabled = adapter.disableRecipesFor(material);
        if (keepRecipe) {
            for (Map.Entry<NamespacedKey, CraftingRecipe> entry : disabled.entrySet()) {
                craftingRegistry.registerRecipe(entry.getKey(), entry.getValue());
            }
        }
    }
}
