package me.mykindos.betterpvp.core.utilities.model.item.banner;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.Value;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import org.bukkit.DyeColor;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Value
@Getter
@Builder(toBuilder = true, builderClassName = "BannerBuilder")
public class BannerWrapper implements ItemProvider {

    private static final Random RANDOM = new Random();
    @NotNull
    @Singular
    List<Pattern> patterns;
    @NotNull @Builder.Default BannerColor baseColor = BannerColor.WHITE;

    @Nonnull
    public static BannerWrapper random() {
        final BannerColor[] banners = BannerColor.values();
        final BannerColor color = banners[RANDOM.nextInt(banners.length)];
        final BannerBuilder wrapper = BannerWrapper.builder().baseColor(color);

        final int patternCount = RANDOM.nextInt(3) + 1;
        final PatternType[] patterns = PatternType.values();
        final DyeColor[] colors = DyeColor.values();
        for (int i = 0; i < patternCount; i++) {
            final PatternType pattern = patterns[RANDOM.nextInt(patterns.length)];
            final DyeColor dyeColor = colors[RANDOM.nextInt(colors.length)];
            wrapper.pattern(new Pattern(dyeColor, pattern));
        }

        return wrapper.build();
    }

    @Nonnull
    public List<Pattern> getPatterns() {
        return Collections.unmodifiableList(this.patterns);
    }

    @Override
    public ItemStack get(@Nullable String lang) {
        return get();
    }

    @Override
    public BannerWrapper clone() {
        return new BannerWrapper(new ArrayList<>(this.patterns), this.baseColor);
    }

    @Override
    public ItemStack get() {
        final ItemStack banner = new ItemStack(this.baseColor.material);
        final BannerMeta meta = (BannerMeta) banner.getItemMeta();
        if (meta == null) {
            throw new IllegalStateException("Banner meta is null");
        }
        meta.setPatterns(this.patterns);
        banner.setItemMeta(meta);
        return banner;
    }

}