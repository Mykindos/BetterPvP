package me.mykindos.betterpvp.core.utilities.model.item.skull;

import com.destroystokyo.paper.profile.PlayerProfile;
import lombok.CustomLog;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;

@CustomLog
public class SkullBuilder {

    private final ItemStack itemStack;

    public SkullBuilder(String texture) {
        PlayerProfile profile = Bukkit.createProfile(null, "a"); // Get a new player profile
        PlayerTextures textures = profile.getTextures();
        URL urlObject;
        try {
            urlObject = new URL(texture); // The URL to the skin, for example: https://textures.minecraft.net/texture/18813764b2abc94ec3c3bc67b9147c21be850cdf996679703157f4555997ea63a
        } catch (MalformedURLException exception) {
            String decoded = new String(Base64.getDecoder().decode(texture));
            // We simply remove the "beginning" and "ending" part of the JSON, so we're left with only the URL. You could use a proper
            // JSON parser for this, but that's not worth it. The String will always start exactly with this stuff anyway
            final String url = decoded.substring("{\"textures\":{\"SKIN\":{\"url\":\"".length(), decoded.length() - "\"}}}".length());
            try {
                urlObject = new URL(url);
            } catch (MalformedURLException e) {
                log.error("Invalid skull texture URL: {}", url, e);
                itemStack = new ItemStack(Material.PLAYER_HEAD);
                return;
            }
        }
        textures.setSkin(urlObject); // Set the skin of the player profile to the URL
        profile.setTextures(textures); // Set the textures back to the profile

        itemStack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) itemStack.getItemMeta();
        meta.setPlayerProfile(profile); // Set the owning player of the head to the player profile
        itemStack.setItemMeta(meta);
    }

    public ItemStack build() {
        return itemStack.clone();
    }

}
