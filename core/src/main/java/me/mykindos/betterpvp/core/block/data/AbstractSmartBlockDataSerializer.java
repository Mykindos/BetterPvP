package me.mykindos.betterpvp.core.block.data;

import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

/**
 * Abstract base class for SmartBlockDataSerializer that automatically handles compression detection and decompression.
 * All smart block serializers should extend this class instead of implementing SmartBlockDataSerializer directly.
 */
@CustomLog
public abstract class AbstractSmartBlockDataSerializer<T> implements SmartBlockDataSerializer<T> {

    /**
     * Subclasses implement this method to serialize their data without worrying about compression.
     * The base class will handle any compression concerns.
     */
    protected abstract byte[] serializeToRawBytes(@NotNull T data) throws IOException;

    /**
     * Subclasses implement this method to deserialize from raw (uncompressed) bytes.
     * The base class will automatically detect and decompress GZIP data if needed.
     */
    @NotNull
    protected abstract T deserializeFromRawBytes(byte[] bytes) throws IOException;

    @Override
    public final byte[] serializeToBytes(@NotNull T data) throws IOException {
        // Delegate to subclass implementation
        return serializeToRawBytes(data);
    }

    @Override
    public final @NotNull T deserializeFromBytes(byte[] bytes) throws IOException {
        // Auto-detect and handle GZIP compression
        byte[] rawBytes = decompressIfNeeded(bytes);
        
        // Delegate to subclass implementation
        return deserializeFromRawBytes(rawBytes);
    }

    /**
     * Detects GZIP compression and decompresses if necessary.
     */
    private byte[] decompressIfNeeded(byte[] bytes) throws IOException {
        // Check for GZIP magic bytes (1F 8B)
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0x1F && (bytes[1] & 0xFF) == 0x8B) {
            log.debug("Detected GZIP compression, decompressing {} bytes", bytes.length);
            
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                 GZIPInputStream gzis = new GZIPInputStream(bais);
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                
                byte[] buffer = new byte[1024];
                int len;
                while ((len = gzis.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                
                byte[] decompressed = baos.toByteArray();
                log.debug("Decompressed from {} to {} bytes", bytes.length, decompressed.length);
                return decompressed;
            }
        }
        
        // Not compressed, return as-is
        return bytes;
    }
}
