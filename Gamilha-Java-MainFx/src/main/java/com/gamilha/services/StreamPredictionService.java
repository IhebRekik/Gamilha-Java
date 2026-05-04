package com.gamilha.services;

import com.gamilha.utils.ConnectionManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class StreamPredictionService {

    private static final String OPENROUTER_KEY = "";

    public Map<String, Object> predictStreams(int userId, int lookbackDays) throws SQLException {
        String sql = userId == 0
                ? "SELECT YEAR(created_at) yr, MONTH(created_at) mo, COUNT(*) cnt FROM stream " +
                  "WHERE created_at >= DATE_SUB(NOW(), INTERVAL ? DAY) GROUP BY yr, mo ORDER BY yr, mo"
                : "SELECT YEAR(created_at) yr, MONTH(created_at) mo, COUNT(*) cnt FROM stream " +
                  "WHERE user_id=? AND created_at >= DATE_SUB(NOW(), INTERVAL ? DAY) GROUP BY yr, mo ORDER BY yr, mo";

        List<Integer> counts = new ArrayList<>();
        Map<String, Integer> monthly = new LinkedHashMap<>();
        try (PreparedStatement ps = ConnectionManager.getConnection().prepareStatement(sql)) {
            if (userId == 0) { ps.setInt(1, lookbackDays); }
            else { ps.setInt(1, userId); ps.setInt(2, lookbackDays); }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int c = rs.getInt("cnt");
                counts.add(c);
                monthly.put(rs.getInt("yr") + "-" + String.format("%02d", rs.getInt("mo")), c);
            }
        }
        double avg = counts.isEmpty() ? 0 : counts.stream().mapToInt(i -> i).average().orElse(0);
        double ws = 0, wt = 0; int n = counts.size();
        for (int i = 0; i < n; i++) { double w = i < n/2 ? 1.0 : 2.0; ws += counts.get(i)*w; wt += w; }
        int pred = (int) Math.round(wt > 0 ? ws/wt : avg);
        String trend = "stable 📊";
        if (n >= 2) {
            int f = counts.subList(0,n/2).stream().mapToInt(i->i).sum();
            int s = counts.subList(n/2,n).stream().mapToInt(i->i).sum();
            if (s > f*1.15) trend = "hausse 📈"; else if (s < f*0.85) trend = "baisse 📉";
        }
        var next = LocalDate.now().plusMonths(1);
        String nextKey = next.getYear() + "-" + String.format("%02d", next.getMonthValue());
        String[] mois = {"Janvier","Février","Mars","Avril","Mai","Juin","Juillet","Août","Septembre","Octobre","Novembre","Décembre"};
        String nextFr = mois[next.getMonthValue()-1] + " " + next.getYear();
        String msg = counts.isEmpty() ? "Pas assez de données." :
                String.format("Prévision pour %s : ~%d stream(s). Moyenne : %.1f/mois. Tendance : %s.", nextFr, pred, avg, trend);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("prediction", pred); r.put("average", avg); r.put("trend", trend);
        r.put("nextMonth", nextKey); r.put("nextMonthFr", nextFr);
        r.put("monthlyData", monthly); r.put("message", msg); r.put("aiMessage", "");
        return r;
    }
}
