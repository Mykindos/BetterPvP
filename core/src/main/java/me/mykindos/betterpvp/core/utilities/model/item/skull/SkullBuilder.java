package me.mykindos.betterpvp.core.utilities.model.item.skull;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * Builds a {@link Material#PLAYER_HEAD} carrying an arbitrary skin.
 * <p>
 * The profile is always given a stable UUID and the texture as a literal {@code textures} property rather than a
 * skin URL on a blank profile: outgoing items are decoded and re-encoded by the packet remapper, and a profile
 * with neither a UUID nor a resolvable name does not survive that round trip - the client renders a bare head.
 */
public class SkullBuilder {

    private static final String TEXTURE_PREFIX = "{\"textures\":{\"SKIN\":{\"url\":\"";
    private static final String TEXTURE_SUFFIX = "\"}}}";

    private final ItemStack itemStack;

    /**
     * @param texture either the base64 texture value or the {@code textures.minecraft.net} skin URL
     */
    public SkullBuilder(String texture) {
        final String encoded = encode(texture);
        // Derived from the texture so the same skin always resolves to the same profile, and two different
        // skins never collide on one.
        final UUID id = UUID.nameUUIDFromBytes(encoded.getBytes(StandardCharsets.UTF_8));

        final PlayerProfile profile = Bukkit.createProfile(id, id.toString().replace("-", "").substring(0, 16));
        profile.setProperty(new ProfileProperty("textures", encoded));

        itemStack = new ItemStack(Material.PLAYER_HEAD);
        final SkullMeta meta = (SkullMeta) itemStack.getItemMeta();
        meta.setPlayerProfile(profile);
        itemStack.setItemMeta(meta);
    }

    /**
     * @return the given texture as a base64 value, wrapping a bare skin URL into one
     */
    private static String encode(String texture) {
        if (!texture.startsWith("http")) {
            return texture;
        }
        return Base64.getEncoder().encodeToString((TEXTURE_PREFIX + texture + TEXTURE_SUFFIX).getBytes(StandardCharsets.UTF_8));
    }

    public ItemStack build() {
        return itemStack.clone();
    }

}
