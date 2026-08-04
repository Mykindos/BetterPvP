package me.mykindos.betterpvp.core.world.schematic;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * A structure: blocks and the Mapper data-points that belong with them, in one file.
 * <p>
 * Two entries in a zip, each written by the serializer that already handles it well — {@code blocks.schem} by
 * FastAsyncWorldEdit, {@code regions.json} by {@link CapturedRegionCodec}. Nothing about block storage is reinvented, so
 * block entities, palettes and future block properties keep working for free.
 * <p>
 * The reason it is one file rather than a {@code .schem} beside a {@code .json} is that the two halves share an anchor.
 * Kept apart, a builder can re-save one and not the other and get a hull whose helm is thirty blocks out to sea, with
 * nothing to warn them. Captured together, they cannot disagree.
 */
public class StructureFormat implements SchematicFormat {

    static final String BLOCKS_ENTRY = "blocks.schem";
    static final String REGIONS_ENTRY = "regions.json";

    private final SchematicFormat blockFormat;

    public StructureFormat() {
        this(new FaweSchematicFormat());
    }

    StructureFormat(@NotNull SchematicFormat blockFormat) {
        this.blockFormat = blockFormat;
    }

    @Override
    public @NotNull String id() {
        return "structure";
    }

    @Override
    public @NotNull Set<String> extensions() {
        return Set.of("structure");
    }

    @Override
    public @NotNull Schematic read(@NotNull InputStream in) throws IOException {
        byte[] blockBytes = null;
        CapturedRegionCodec.Document document = null;

        final ZipInputStream zip = new ZipInputStream(in);
        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            switch (entry.getName()) {
                case BLOCKS_ENTRY -> blockBytes = zip.readAllBytes();
                case REGIONS_ENTRY -> document = CapturedRegionCodec.read(new ByteArrayInputStream(zip.readAllBytes()));
                default -> {
                    // Unknown entries are skipped rather than rejected, so a later addition to the format stays
                    // readable by an older server instead of taking the whole structure down with it.
                }
            }
        }

        if (blockBytes == null) {
            throw new IOException("Structure has no " + BLOCKS_ENTRY + " entry");
        }

        final Schematic blocks = blockFormat.read(new ByteArrayInputStream(blockBytes));
        if (document == null) {
            return blocks;
        }
        return blocks.withRegions(document.getRegions(), document.getAnchorYaw());
    }

    @Override
    public void write(@NotNull OutputStream out, @NotNull Schematic schematic) throws IOException {
        final ZipOutputStream zip = new ZipOutputStream(out);

        zip.putNextEntry(new ZipEntry(BLOCKS_ENTRY));
        final ByteArrayOutputStream blocks = new ByteArrayOutputStream();
        blockFormat.write(blocks, schematic);
        zip.write(blocks.toByteArray());
        zip.closeEntry();

        zip.putNextEntry(new ZipEntry(REGIONS_ENTRY));
        CapturedRegionCodec.write(zip, schematic.getRegions(), schematic.getAnchorYaw());
        zip.closeEntry();

        zip.finish();
    }
}
