package me.mykindos.betterpvp.core.inventory.inventoryaccess.impl;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.abstraction.util.ItemUtils;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.component.ComponentWrapper;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.util.ReflectionRegistry;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.util.ReflectionUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * Items are stored in Paper's byte format: gzip-compressed NBT carrying its DataVersion, upgraded by Paper on read.
 * The uncompressed form is the same NBT without the gzip layer.
 */
@CustomLog
public class ItemUtilsImpl implements ItemUtils {

    @Override
    public byte[] serializeItemStack(@NotNull ItemStack itemStack, boolean compressed) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializeItemStack(itemStack, out, compressed);
        return out.toByteArray();
    }

    @Override
    public void serializeItemStack(@NotNull ItemStack itemStack, @NotNull OutputStream outputStream, boolean compressed) {
        byte[] bytes = itemStack.serializeAsBytes();
        try {
            if (compressed) {
                outputStream.write(bytes);
            } else {
                try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
                    in.transferTo(outputStream);
                }
            }
        } catch (IOException e) {
            log.error("Failed to serialize item stack", e).submit();
        }
    }

    @Override
    public ItemStack deserializeItemStack(byte[] data, boolean compressed) {
        return deserializeItemStack(new ByteArrayInputStream(data), compressed);
    }

    @Override
    public ItemStack deserializeItemStack(@NotNull InputStream inputStream, boolean compressed) {
        try {
            CompoundTag nbt = compressed
                    ? NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap())
                    : NbtIo.read(new DataInputStream(inputStream));
            // A blob without a DataVersion is read as 1.20.4 data.
            if (nbt.getInt("DataVersion").isEmpty()) {
                nbt.putInt("DataVersion", 3700);
            }

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            NbtIo.writeCompressed(nbt, bytes);
            return ItemStack.deserializeBytes(bytes.toByteArray());
        } catch (IOException | IllegalStateException e) {
            log.error("Failed to deserialize item stack", e).submit();
        }

        return null;
    }

    @Override
    public void setDisplayName(@NotNull ItemMeta itemMeta, @NotNull ComponentWrapper name) {
        ReflectionUtils.setFieldValue(
            ReflectionRegistry.CB_CRAFT_META_ITEM_DISPLAY_NAME_FIELD,
            itemMeta,
            InventoryUtilsImpl.createNMSComponent(name)
        );
    }
    
    @Override
    public void setLore(@NotNull ItemMeta itemMeta, @NotNull List<@NotNull ComponentWrapper> lore) {
        ReflectionUtils.setFieldValue(
            ReflectionRegistry.CB_CRAFT_META_ITEM_LORE_FIELD,
            itemMeta,
            lore.stream().map(InventoryUtilsImpl::createNMSComponent).collect(Collectors.toList())
        );
    }
    
}