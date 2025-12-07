package me.mykindos.betterpvp.clans.clans.explosion;

import lombok.Getter;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

@Getter
public enum ExplosiveResistantBlocks {

    STONEBRICK(Material.STONE_BRICKS, Material.CRACKED_STONE_BRICKS),
    NETHERBRICKS(Material.NETHER_BRICKS, Material.NETHERRACK),
    SANDSTONE(Material.SMOOTH_SANDSTONE, Material.SANDSTONE),
    REDSANDSTONE(Material.SMOOTH_RED_SANDSTONE, Material.RED_SANDSTONE),
    BLACKSTONE(Material.POLISHED_BLACKSTONE_BRICKS, Material.CRACKED_POLISHED_BLACKSTONE_BRICKS),
    QUARTZ(Material.QUARTZ_BRICKS, Material.CHISELED_QUARTZ_BLOCK),
    PURPUR(Material.PURPUR_BLOCK, Material.PURPUR_PILLAR),
    ENDSTONE(Material.END_STONE_BRICKS, Material.END_STONE),
    MOSSYSTONEBRICK(Material.MOSSY_STONE_BRICKS, Material.MOSSY_COBBLESTONE),
    GLASS(Material.TINTED_GLASS, Material.GLASS),
    PRISMARINE(Material.DARK_PRISMARINE, Material.PRISMARINE_BRICKS, Material.PRISMARINE),
    /*
    To replace prismarine when 1.21.0 comes out, these blocks have 3 variants, look better, and can also be used as the vault block

    COPPER(Material.CHISELED_COPPER, Material.COPPER_BLOCK, Material.COPPER_GRATE),
    COPPEREXPOSED(Material.EXPOSED_CHISELED_COPPER, Material.EXPOSED_COPPER_BLOCK, Material.EXPOSED_COPPER_GRATE),
    COPPERWEATHERED(Material.WEATHERED_CHISELED_COPPER, Material.WEATHERED_COPPER_BLOCK, Material.WEATHERED_COPPER_GRATE),
    COPPEROXIDIZED(Material.OXIDIZED_CHISELED_COPPER, Material.OXIDIZED_COPPER_BLOCK, Material.OXIDIZED_COPPER_GRATE),
    COPPERWAXED(Material.WAXED_CHISELED_COPPER, Material.WAXED_COPPER_BLOCK, Material.WAXED_COPPER_GRATE),
    COPPERWAXEDEXPOSED(Material.WAXED_EXPOSED_CHISELED_COPPER, Material.WAXED_EXPOSED_COPPER_BLOCK, Material.WAXED_EXPOSED_COPPER_GRATE),
    COPPERWAXEDWEATHERED(Material.WAXED_WEATHERED_CHISELED_COPPER, Material.WAXED_WEATHERED_COPPER_BLOCK, Material.WAXED_WEATHERED_COPPER_GRATE),
    COPPERWAXEDOXIDIZED(Material.WAXED_OXIDIZED_CHISELED_COPPER, Material.WAXED_OXIDIZED_COPPER_BLOCK, Material.WAXED_OXIDIZED_COPPER_GRATE),

    TUFFBRICKS(Material.TUFF_BRICKS, Material.TUFF),

    PRISMARINE(Material.PRISMARINE_BRICKS, Material.PRISMARINE),
    */
    MUDBRICKS(Material.MUD_BRICKS, Material.MUD),
    DEEPSLATEBRICKS(Material.DEEPSLATE_BRICKS, Material.DEEPSLATE);


    private final List<Material> tiers;

    ExplosiveResistantBlocks(final Material... tiers) {
        this.tiers = Arrays.asList(tiers);
    }


}
