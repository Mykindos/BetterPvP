package me.mykindos.betterpvp.clans.clans.pillage;

import lombok.Data;
import me.mykindos.betterpvp.core.components.clans.IClan;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Date;
import java.util.Random;

@Data
public class Pillage {

    private final IClan pillager;
    private final IClan pillaged;
    private final BossBar siegedBar;
    private final BossBar siegingBar;
    private long pillageFinishTime;
    private long absolutionFinishTime;
    private long pillageStartTime;

    public Pillage(IClan pillager, IClan pillaged) {
        this.pillager = pillager;
        this.pillaged = pillaged;

        final BossBar.Color color = BossBar.Color.values()[new Random().nextInt(BossBar.Color.values().length)];
        this.siegedBar = BossBar.bossBar(Component.empty(),
                1.0f,
                color,
        BossBar.Overlay.NOTCHED_20);

        this.siegingBar = BossBar.bossBar(Component.empty(),
                1.0f,
                color,
                BossBar.Overlay.NOTCHED_20);
    }

    public void updateBossBar() {
        final long remainingMillis = pillageFinishTime - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            return;
        }

        final float progress = (float) remainingMillis / (absolutionFinishTime - pillageStartTime);
        if (progress >= 0 && progress <= 1) {
            siegedBar.progress(progress);
            siegingBar.progress(progress);
        }

        final Date date = new Date(remainingMillis);
        final String time = String.format("%dm %ds", date.getMinutes(), date.getSeconds());

        siegedBar.name(Component.text("Sieged by", NamedTextColor.GRAY)
                .appendSpace()
                .append(Component.text(pillager.getName(), NamedTextColor.RED))
                .append(Component.text(" for ", NamedTextColor.GRAY))
                .append(Component.text(time, NamedTextColor.YELLOW)));

        siegingBar.name(Component.text("Sieging", NamedTextColor.GRAY)
                .appendSpace()
                    .append(Component.text(pillaged.getName(), NamedTextColor.GREEN))
                .append(Component.text(" for " , NamedTextColor.GRAY))
                .append(Component.text(time, NamedTextColor.YELLOW)));
    }
}
