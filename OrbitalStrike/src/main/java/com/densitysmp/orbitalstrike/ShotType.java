package com.densitysmp.orbitalstrike;

public enum ShotType {
    STAB("stab"),
    NUKE("nuke"),
    WOLF("wolf"),
    STASIS("stasis"),
    STASIS_AIM("stasis_aim"),
    RAILGUN("railgun"),
    COMPRESSED_NUKE("compressed_nuke"),
    CROSS("cross"),
    X_SHOT("x_shot"),
    RING("ring"),
    LINE("line"),
    HEXAGON("hexagon"),
    PENTAGON("pentagon"),
    STAR("star"),
    STAR_OF_DAVID("star_of_david"),
    HEART("heart"),
    SCATTER("scatter"),
    GRID("grid"),
    TEXT("text"),
    GOLEM("golem"),
    WITHER("wither"),
    CLUSTER("cluster"),
    WITHER_SKULL("wither_skull");

    private final String id;

    ShotType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** Fires on cast instead of on reel-in, and doesn't throw a bobber. */
    public boolean isInstant() {
        return this == WOLF || this == STASIS || this == STASIS_AIM || this == GOLEM || this == WITHER;
    }

    /** Doesn't need a targeted block; acts around the player. */
    public boolean needsNoTarget() {
        return this == WOLF || this == STASIS || this == GOLEM || this == WITHER;
    }

    public boolean supportsSlam() {
        return switch (this) {
            case NUKE, COMPRESSED_NUKE, CROSS, X_SHOT, RING, LINE, HEXAGON, PENTAGON, STAR,
                 STAR_OF_DAVID, HEART, SCATTER, GRID, TEXT, CLUSTER -> true;
            default -> false;
        };
    }

    public static ShotType fromId(String id) {
        for (ShotType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}
