package me.mykindos.betterpvp.core.inventory.inventoryaccess.impl;

import com.mojang.serialization.Dynamic;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.abstraction.util.ItemUtils;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.component.ComponentWrapper;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.util.ReflectionRegistry;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.util.ReflectionUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.CraftRegistry;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;

@CustomLog
public class ItemUtilsImpl implements ItemUtils {
    
    @Override
    public byte[] serializeItemStack(org.bukkit.inventory.@NotNull ItemStack itemStack, boolean compressed) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializeItemStack(itemStack, out, compressed);
        return out.toByteArray();
    }
    
    @Override
    public void serializeItemStack(org.bukkit.inventory.@NotNull ItemStack itemStack, @NotNull OutputStream outputStream, boolean compressed) {
        try {
            ItemStack nmsStack = CraftItemStack.asNMSCopy(itemStack);
            CompoundTag nbt = (CompoundTag) nmsStack.save(CraftRegistry.getMinecraftRegistry(), new CompoundTag());
            
            if (compressed) {
                NbtIo.writeCompressed(nbt, outputStream);
            } else {
                DataOutputStream dataOut = new DataOutputStream(outputStream);
                NbtIo.write(nbt, dataOut);
            }
            
            outputStream.flush();
        } catch (IOException e) {
            log.error("Failed to serialize item stack", e).submit();
        }
    }
    
    @Override
    public org.bukkit.inventory.ItemStack deserializeItemStack(byte[] data, boolean compressed) {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        return deserializeItemStack(in, compressed);
    }
    
    @Override
    public org.bukkit.inventory.ItemStack deserializeItemStack(@NotNull InputStream inputStream, boolean compressed) {
        try {
            CompoundTag nbt;
            if (compressed) {
                nbt = NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap());
            } else {
                DataInputStream dataIn = new DataInputStream(inputStream);
                nbt = NbtIo.read(dataIn);
            }
            
            Dynamic<Tag> converted = DataFixers.getDataFixer()
                .update(
                    References.ITEM_STACK,
                    new Dynamic<>(NbtOps.INSTANCE, nbt),
                    3700, CraftMagicNumbers.INSTANCE.getDataVersion()
                );
            
            ItemStack itemStack = ItemStack.parse(
                CraftRegistry.getMinecraftRegistry(),
                converted.getValue()
            ).orElse(ItemStack.EMPTY);
            return CraftItemStack.asCraftMirror(itemStack);
        } catch (IOException e) {
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