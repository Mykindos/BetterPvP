package me.mykindos.betterpvp.core.world.logger;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WorldLogTimeFormat {


        public static final List<Long> times = Arrays.asList(
                TimeUnit.DAYS.toMillis(365),
                TimeUnit.DAYS.toMillis(7),
                TimeUnit.DAYS.toMillis(1),
                TimeUnit.HOURS.toMillis(1),
                TimeUnit.MINUTES.toMillis(1),
                TimeUnit.SECONDS.toMillis(1) );
        public static final List<String> timesString = Arrays.asList("y","w","d","h","m","s");

    public static String toDuration(Instant timestamp) {
        long duration = Duration.between(timestamp, Instant.now()).toMillis();

        StringBuilder res = new StringBuilder();
        for (int i = 0; i < times.size(); i++) {
            Long current = times.get(i);
            if (duration >= current) {
                double temp = (double) duration / current;
                res.append(String.format("%.2f", temp)).append("/").append(timesString.get(i)).append(" ago");
                break;
            }
        }
        if ("".equals(res.toString()))
            return "0/s ago";
        else
            return res.toString();
    }

}
