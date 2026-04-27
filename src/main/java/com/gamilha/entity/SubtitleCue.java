package com.gamilha.entity;

public class SubtitleCue {

    private final long startMillis;
    private final long endMillis;
    private final String text;

    public SubtitleCue(long startMillis, long endMillis, String text) {
        this.startMillis = startMillis;
        this.endMillis = endMillis;
        this.text = text;
    }

    public long getStartMillis() {
        return startMillis;
    }

    public long getEndMillis() {
        return endMillis;
    }

    public String getText() {
        return text;
    }

    public boolean contains(long positionMillis) {
        return positionMillis >= startMillis && positionMillis <= endMillis;
    }
}
