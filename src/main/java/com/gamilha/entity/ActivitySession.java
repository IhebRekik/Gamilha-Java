package com.gamilha.entity;

import java.sql.Timestamp;

/**
 * Entité représentant une session d'activité utilisateur.
 */
public class ActivitySession {

    private int id;
    private int userId;
    private Timestamp sessionStart;
    private Timestamp sessionEnd;
    private Integer durationSec; // null si session en cours

    public ActivitySession() {}

    public ActivitySession(int userId) {
        this.userId       = userId;
        this.sessionStart = new Timestamp(System.currentTimeMillis());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Durée en secondes (calculée si session encore ouverte). */
    public long getEffectiveDurationSec() {
        if (durationSec != null) return durationSec;
        if (sessionStart == null) return 0;
        long end = sessionEnd != null ? sessionEnd.getTime() : System.currentTimeMillis();
        return (end - sessionStart.getTime()) / 1000L;
    }

    /** Formate une durée en secondes → "2h 14min" ou "38 min" ou "45 sec". */
    public static String formatDuration(long totalSec) {
        if (totalSec <= 0) return "0 sec";
        long h   = totalSec / 3600;
        long m   = (totalSec % 3600) / 60;
        long s   = totalSec % 60;
        if (h > 0)  return h + "h " + m + "min";
        if (m > 0)  return m + "min " + s + "sec";
        return s + " sec";
    }

    // ── Getters / Setters ─────────────────────────────────────────────────

    public int getId()                        { return id; }
    public void setId(int id)                 { this.id = id; }

    public int getUserId()                    { return userId; }
    public void setUserId(int userId)         { this.userId = userId; }

    public Timestamp getSessionStart()        { return sessionStart; }
    public void setSessionStart(Timestamp t)  { this.sessionStart = t; }

    public Timestamp getSessionEnd()          { return sessionEnd; }
    public void setSessionEnd(Timestamp t)    { this.sessionEnd = t; }

    public Integer getDurationSec()           { return durationSec; }
    public void setDurationSec(Integer d)     { this.durationSec = d; }
}
