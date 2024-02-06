package me.mykindos.betterpvp.core.utilities;

import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

/**
 * From <a href="https://raw.githubusercontent.com/lukalt/PacketWrapper/master/src/main/java/com/comphenix/packetwrapper/util/MoreConverters.java">ProtocolWrapper</a>
 * @author Lukas Alt
 * @since 08.05.2023
 */
public class UtilConverter {

    public static <V> EquivalentConverter<Int2ObjectMap<V>> getInt2ObjectMapConverter(EquivalentConverter<V> valConverter) {
        return new EquivalentConverter<>() {
            @Override
            public Int2ObjectMap<V> getSpecific(Object generic) {
                Map<Object, Object> genericMap = (Map<Object, Object>) generic;
                Int2ObjectMap<V> newMap;

                try {
                    newMap = (Int2ObjectMap<V>) genericMap.getClass().newInstance();
                } catch (ReflectiveOperationException ex) {
                    newMap = new Int2ObjectOpenHashMap<>();
                }

                for (Map.Entry<Object, Object> entry : genericMap.entrySet()) {
                    newMap.put(((Integer) entry.getKey()).intValue(), valConverter.getSpecific(entry.getValue()));
                }

                return newMap;
            }

            @Override
            public Object getGeneric(Int2ObjectMap<V> specific) {
                Map<Object, Object> newMap;

                try {
                    newMap = specific.getClass().newInstance();
                } catch (ReflectiveOperationException ex) {
                    newMap = new HashMap<>();
                }

                for (Int2ObjectMap.Entry<V> entry : specific.int2ObjectEntrySet()) {
                    newMap.put(entry.getIntKey(), valConverter.getGeneric(entry.getValue()));
                }

                return newMap;
            }

            @Override
            public Class<Int2ObjectMap<V>> getSpecificType() {
                return null;
            }
        };
    }

    private static final Supplier<EquivalentConverter<Material>> MATERIAL_CONVERTER = Suppliers.memoize(() -> {
        Class<?> CRAFT_MAGIC_NUMBERS_CLASS = MinecraftReflection.getCraftBukkitClass("util.CraftMagicNumbers");
        MethodAccessor ITEM_TO_MATERIAL_ACCESSOR = Accessors.getMethodAccessor(CRAFT_MAGIC_NUMBERS_CLASS, "getMaterial", MinecraftReflection.getItemClass());
        MethodAccessor MATERIAL_TO_ITEM_ACCESSOR = Accessors.getMethodAccessor(CRAFT_MAGIC_NUMBERS_CLASS, "getItem", Material.class);
        return new EquivalentConverter<>() {
            @Override
            public Object getGeneric(Material specific) {
                return MATERIAL_TO_ITEM_ACCESSOR.invoke(null, specific);
            }

            @Override
            public Material getSpecific(Object generic) {
                return (Material) ITEM_TO_MATERIAL_ACCESSOR.invoke(null, generic);
            }

            @Override
            public Class<Material> getSpecificType() {
                return Material.class;
            }
        };
    });

    public static EquivalentConverter<Material> getMaterialConverter() {
        return MATERIAL_CONVERTER.get();
    }
}
