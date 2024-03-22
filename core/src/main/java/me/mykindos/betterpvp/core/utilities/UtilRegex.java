package me.mykindos.betterpvp.core.utilities;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UtilRegex {
    public static final String UUID = "[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}";
    public static final String CLANNAME = "([a-zA-Z0-9])";

    public static final String PLAYERNAME = "([a-zA-Z0-9_]){1,16}";

    public static String Clanname(int min, int max) {
        return UtilRegex.CLANNAME + "{" + min + "," + max + "}";
    }
}
