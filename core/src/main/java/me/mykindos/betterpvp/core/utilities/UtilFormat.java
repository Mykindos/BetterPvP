package me.mykindos.betterpvp.core.utilities;

import java.text.DecimalFormat;

public class UtilFormat {

    private static final DecimalFormat FORMATTER = new DecimalFormat("#,###");

    public static String formatNumber(int num) {
        return FORMATTER.format(num);
    }

}
