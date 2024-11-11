package me.mykindos.betterpvp.core.framework.persistence;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * A {@link PersistentDataType} for {@link ConfigurationSerializable}s
 * @param <T> The type of the {@link ConfigurationSerializable}
 */
public class ConfigurationSerializableDataType<T extends ConfigurationSerializable> implements PersistentDataType<byte[], T> {
    private final Class<T> type;

    /**
     * Creates a new {@link ConfigurationSerializableDataType} for the given type
     * @param type The type of the {@link ConfigurationSerializable}
     */
    public ConfigurationSerializableDataType(final Class<T> type) {
        this.type = type;
    }

    @NotNull
    @Override
    public Class<byte[]> getPrimitiveType() {
        return byte[].class;
    }

    @NotNull
    @Override
    public Class<T> getComplexType() {
        return type;
    }

    @NotNull
    @Override
    public byte [] toPrimitive(@NotNull final T serializable, @NotNull final PersistentDataAdapterContext persistentDataAdapterContext) {
        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); final BukkitObjectOutputStream bukkitObjectOutputStream = new BukkitObjectOutputStream(outputStream)) {
            bukkitObjectOutputStream.writeObject(serializable);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(getExceptionMessage(type, SerializationType.SERIALIZATION), e);
        }
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public T fromPrimitive(@NotNull final byte [] bytes, @NotNull final PersistentDataAdapterContext persistentDataAdapterContext) {
        try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes); final BukkitObjectInputStream bukkitObjectInputStream = new BukkitObjectInputStream(inputStream)) {
            return (T) bukkitObjectInputStream.readObject();
        } catch (IOException e) {
            throw new UncheckedIOException(getExceptionMessage(type, SerializationType.DESERIALIZATION), e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(getExceptionMessage(type, SerializationType.DESERIALIZATION), e);
        }
    }

    private static boolean isBukkitClass(final Class<?> clazz) {
        return clazz.getPackage().getName().startsWith("org.bukkit.");
    }

    static String getExceptionMessage(Class<? extends ConfigurationSerializable> type, SerializationType serializationType) {
        String msg = "Could not " + serializationType + " object of type " + type.getName() + ".";
        if(!isBukkitClass(type)) {
            msg += " This is not a bug in MorePersistentDataTypes, but a bug in your " + serializationType + ".";
            if(serializationType == SerializationType.DESERIALIZATION) {
                msg += " Make sure that your class is properly registered for deserialization using org.bukkit.configuration.serialization.ConfigurationSerialization#registerClass(Class).";
            }
        }
        return msg;
    }

    enum SerializationType {
        SERIALIZATION("serialization"),
        DESERIALIZATION("deserialization");

        private final String fancyName;

        SerializationType(String fancyName) {
            this.fancyName = fancyName;
        }

        @Override
        public String toString() {
            return fancyName;
        }
    }
}