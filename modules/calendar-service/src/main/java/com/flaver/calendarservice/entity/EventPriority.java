package com.flaver.calendarservice.entity;

public enum EventPriority {
    REQUIRED("обязательный"),
    IMPORTANT("важный"),
    OPTIONAL("необязательный");

    private final String displayNameRu;

    EventPriority(String displayNameRu) {
        this.displayNameRu = displayNameRu;
    }

    public String getDisplayNameRu() {
        return displayNameRu;
    }
}
