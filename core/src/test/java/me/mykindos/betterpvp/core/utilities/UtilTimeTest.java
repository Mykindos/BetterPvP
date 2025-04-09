package me.mykindos.betterpvp.core.utilities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Locale;

class UtilTimeTest {

    private static final long SECOND = 1000L;
    private static final long MINUTE = SECOND * 60;
    private static final long HOUR = MINUTE * 60;
    private static final long DAY = HOUR * 24;
    private static final long YEAR = DAY * 365;

    @Test
    @DisplayName("Elapsed Test")
    void elapsed() {
        long pastTime = System.currentTimeMillis() - 10000L;
        boolean trueElapsed = UtilTime.elapsed(pastTime, 5000L);
        boolean falseElapsed = UtilTime.elapsed(pastTime, 15000L);
        Assertions.assertTrue(trueElapsed);
        Assertions.assertFalse(falseElapsed);
    }

    @Test
    @DisplayName("Trim Test")
    void trim() {
        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.Category.DISPLAY, Locale.ENGLISH);

        double value1 = UtilTime.trim(2.56024d, 2);
        Assertions.assertEquals(2.56d, value1);

        double value2 = UtilTime.trim(3.7890234, 1);
        Assertions.assertEquals(3.8d, value2);

        Locale.setDefault(Locale.Category.DISPLAY, defaultLocale);
    }

    @Test
    @DisplayName("Convert Test")
    void convert() {
        double secondConversion1 = UtilTime.convert(SECOND * 75 + (double) SECOND /2, UtilTime.TimeUnit.SECONDS, 1);
        Assertions.assertEquals(75.5d, secondConversion1);

        double secondConversion2 = UtilTime.convert(SECOND * 43 + (double) SECOND /3, UtilTime.TimeUnit.SECONDS, 3);
        Assertions.assertEquals(43.333d, secondConversion2);

        double minuteConversion = UtilTime.convert(MINUTE * 4, UtilTime.TimeUnit.MINUTES, 1);
        Assertions.assertEquals(4d, minuteConversion);

        double hourConversion = UtilTime.convert(HOUR * 2 + (double) HOUR /4, UtilTime.TimeUnit.HOURS, 5);
        Assertions.assertEquals(2.25d, hourConversion);

        double dayConversion = UtilTime.convert(DAY * 25, UtilTime.TimeUnit.DAYS, 0);
        Assertions.assertEquals(25d, dayConversion);

        double yearConversion = UtilTime.convert(YEAR * 2 + (double) YEAR /8, UtilTime.TimeUnit.YEARS, 5);
        Assertions.assertEquals(2.125d, yearConversion);

        double bestConversion1 = UtilTime.convert(MINUTE * 59, UtilTime.TimeUnit.BEST, 1);
        Assertions.assertEquals(59d, bestConversion1);

        double bestConversion2 = UtilTime.convert(DAY, UtilTime.TimeUnit.BEST, 1);
        Assertions.assertEquals(1d, bestConversion2);

        double bestConversion3 = UtilTime.convert(DAY * 354, UtilTime.TimeUnit.BEST, 1);
        Assertions.assertEquals(354d, bestConversion3);

    }

    @Test
    @DisplayName("getTimeUnit test")
    void getTimeUnit() {
        String seconds = UtilTime.getTimeUnit(UtilTime.TimeUnit.SECONDS);
        String secondsShort = UtilTime.getTimeUnit("s");
        Assertions.assertEquals("Seconds", seconds);
        Assertions.assertEquals("Seconds", secondsShort);

        String minutes = UtilTime.getTimeUnit(UtilTime.TimeUnit.MINUTES);
        String minutesShort = UtilTime.getTimeUnit("m");
        Assertions.assertEquals("Minutes", minutes);
        Assertions.assertEquals("Minutes", minutesShort);

        String hours = UtilTime.getTimeUnit(UtilTime.TimeUnit.HOURS);
        String hoursShort = UtilTime.getTimeUnit("h");
        Assertions.assertEquals("Hours", hours);
        Assertions.assertEquals("Hours", hoursShort);

        String days = UtilTime.getTimeUnit(UtilTime.TimeUnit.DAYS);
        String daysShort = UtilTime.getTimeUnit("d");
        Assertions.assertEquals("Days", days);
        Assertions.assertEquals("Days", daysShort);

        String years = UtilTime.getTimeUnit(UtilTime.TimeUnit.YEARS);
        String yearsShort = UtilTime.getTimeUnit("y");
        Assertions.assertEquals("Years", years);
        Assertions.assertEquals("Years", yearsShort);

        String best = UtilTime.getTimeUnit(UtilTime.TimeUnit.BEST);
        String bestShort = UtilTime.getTimeUnit("b");
        Assertions.assertEquals("", best);
        Assertions.assertEquals("", bestShort);

    }

    @Test
    @DisplayName("getTimeUnit2 Test")
    void getTimeUnit2() {
        String second = UtilTime.getTimeUnit2(SECOND * 30);
        Assertions.assertEquals("Seconds", second);

        String minute = UtilTime.getTimeUnit2(MINUTE * 30);
        Assertions.assertEquals("Minutes", minute);

        String hour = UtilTime.getTimeUnit2(HOUR * 12);
        Assertions.assertEquals("Hours", hour);

        String day = UtilTime.getTimeUnit2(DAY * 12);
        Assertions.assertEquals("Days", day);

        String year = UtilTime.getTimeUnit2(YEAR * 2);
        Assertions.assertEquals("Years", year);
    }

    @Test
    @DisplayName("getTime Test")
    void getTime() {
        String bestConversion1 = UtilTime.getTime(MINUTE * 59, 1);
        Assertions.assertEquals("59.0 Minutes", bestConversion1);

        String bestConversion2 = UtilTime.getTime(DAY, 1);
        Assertions.assertEquals("1.0 Days", bestConversion2);

        String bestConversion3 = UtilTime.getTime(DAY * 354, 1);
        Assertions.assertEquals("354.0 Days", bestConversion3);
    }

    @Test
    @DisplayName("getTime2 Test")
    void getTime2() {
        String secondConversion1 = UtilTime.getTime2(SECOND * 75 + (double) SECOND /2, UtilTime.TimeUnit.SECONDS, 1);
        Assertions.assertEquals("75.5 Seconds", secondConversion1);

        String secondConversion2 = UtilTime.getTime2(SECOND * 43 + (double) SECOND /3, UtilTime.TimeUnit.SECONDS, 3);
        Assertions.assertEquals("43.333 Seconds", secondConversion2);

        String minuteConversion = UtilTime.getTime2(MINUTE * 4, UtilTime.TimeUnit.MINUTES, 1);
        Assertions.assertEquals("4.0 Minutes", minuteConversion);

        String hourConversion = UtilTime.getTime2(HOUR * 2 + (double) HOUR /4, UtilTime.TimeUnit.HOURS, 5);
        Assertions.assertEquals("2.25 Hours", hourConversion);

        String dayConversion = UtilTime.getTime2(DAY * 25, UtilTime.TimeUnit.DAYS, 0);
        Assertions.assertEquals("25.0 Days", dayConversion);

        String yearConversion = UtilTime.getTime2(YEAR * 2 + (double) YEAR /8, UtilTime.TimeUnit.YEARS, 5);
        Assertions.assertEquals("2.125 Years", yearConversion);

        String bestConversion1 = UtilTime.getTime2(MINUTE * 59, UtilTime.TimeUnit.BEST, 1);
        Assertions.assertEquals("59.0 Minutes", bestConversion1);

        String bestConversion2 = UtilTime.getTime2(DAY, UtilTime.TimeUnit.BEST, 1);
        Assertions.assertEquals("1.0 Days", bestConversion2);

        String bestConversion3 = UtilTime.getTime2(DAY * 354, UtilTime.TimeUnit.BEST, 1);
        Assertions.assertEquals("354.0 Days", bestConversion3);
    }
}