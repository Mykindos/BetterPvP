package me.mykindos.betterpvp.core.block.data;

/**
 * Represents a block with data storage capabilities.
 */
public interface DataHolder<T> {

    /**
     * Gets the data type this block supports, if any.
     * Override this method in subclasses that need block data.
     *
     * @return Optional containing the data type class, empty if no data is supported
     */
    Class<T> getDataType();

    /**
     * Gets the serializer for this block's data, if any.
     * Override this method in subclasses that need block data.
     *
     * @return Optional containing the data serializer, empty if no data is supported
     */
    SmartBlockDataSerializer<T> getDataSerializer();

    /**
     * Creates default data for this block type.
     * Override this method in subclasses that need block data.
     *
     * @return The default data object
     * @throws UnsupportedOperationException if the block does not support data
     */
    @SuppressWarnings("unchecked")
    T createDefaultData();

}
