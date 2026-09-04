package com.flaver.calendarservice.entity;

public enum CompetitionLevel {
    INTERNATIONAL("международные"),
    NATIONAL("всероссийские"),
    REGIONAL("региональные"),
    LOCAL("местные"),
    TRAINING("тренировочные"),
    OTHER("другое");

    private final String displayNameRu;

    CompetitionLevel(String displayNameRu) {
        this.displayNameRu = displayNameRu;
    }

    public String getDisplayNameRu() {
        return displayNameRu;
    }
}
