package me.mykindos.betterpvp.shops.auctionhouse.data;

import lombok.Getter;

@Getter
public enum ListingDuration {

    TWELVE_HOURS("12 Hours", 3600000 * 12L),
    ONE_DAY("1 Day", 3600000 * 24L),
    TWO_DAYS("2 Days", 3600000 * 48L),
    ONE_WEEK("1 Week", 3600000 * 168L);

    private final String display;
    private final long duration;

    ListingDuration(String display, long duration) {
        this.display = display;
        this.duration = duration;
    }
}
