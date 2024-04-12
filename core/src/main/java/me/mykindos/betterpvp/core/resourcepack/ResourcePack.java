package me.mykindos.betterpvp.core.resourcepack;

import lombok.Data;

import java.util.UUID;

@Data
public class ResourcePack {

    private final UUID uuid;
    private final String url;
    private final String hash;
    private byte[] hashBytes;

    public byte[] getHashBytes() {
        if (hashBytes != null) {
            return hashBytes;
        }
        int len = hash.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hash.charAt(i), 16) << 4)
                    + Character.digit(hash.charAt(i+1), 16));
        }
        hashBytes = data;
        return hashBytes;
    }

}
