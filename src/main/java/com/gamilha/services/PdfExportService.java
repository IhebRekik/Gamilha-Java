package com.gamilha.services;

import com.gamilha.entity.ActivitySession;
import com.gamilha.entity.User;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Génération d'un profil HTML/PDF professionnel pour l'utilisateur.
 *
 * Le fichier contient :
 *  - En-tête Gamilha avec couleurs de marque
 *  - Informations personnelles
 *  - Statistiques d'activité (temps total, sessions, moyenne)
 *  - Pied de page avec date de génération
 */
public class PdfExportService {

    private static final String OUTPUT_DIR =
            System.getProperty("user.home") + "/Documents/Gamilha/";

    private final ActivityService activityService = ActivityService.getInstance();

    /**
     * Génère le profil et retourne le chemin du fichier créé.
     *
     * @param user utilisateur dont on exporte le profil
     * @return chemin absolu du fichier HTML généré, ou null en cas d'erreur
     */
    public String generateUserProfilePdf(User user) {
        if (user == null) return null;

        try { Files.createDirectories(Paths.get(OUTPUT_DIR)); }
        catch (IOException e) {
            System.err.println("⚠ Impossible de créer le dossier : " + OUTPUT_DIR);
        }

        String fileName = "profil_" + user.getName().replaceAll("[^a-zA-Z0-9]", "_")
                + "_" + System.currentTimeMillis() + ".html";
        String filePath = OUTPUT_DIR + fileName;

        // ── Récupérer les données d'activité ──────────────────────────────
        long totalSec  = activityService.getTotalTimeSeconds(user.getId());
        long todaySec  = activityService.getTodayTimeSeconds(user.getId());
        long avgSec    = activityService.getAverageSessionSeconds(user.getId());
        int  totalSess = activityService.getTotalSessions(user.getId());

        String now = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String createdAt = user.getCreatedAt() != null
                ? user.getCreatedAt().toLocalDateTime()
                       .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "—";
        String lastSeen = user.getLastSeen() != null
                ? user.getLastSeen().toLocalDateTime()
                       .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "—";
        String role   = user.isAdmin() ? "Administrateur" : "Utilisateur";
        String status = user.isActive() ? "✅ Actif" : "❌ Inactif";

        // ── Génération HTML ───────────────────────────────────────────────
        String html = "<!DOCTYPE html>\n"
            + "<html lang='fr'>\n"
            + "<head>\n"
            + "<meta charset='UTF-8'>\n"
            + "<title>Profil Gamilha - " + user.getName() + "</title>\n"
            + "<style>\n"
            + "  * { margin:0; padding:0; box-sizing:border-box; }\n"
            + "  body { font-family:'Segoe UI',Arial,sans-serif; background:#0d1117; color:#e2e8f0; }\n"
            + "  .page { max-width:900px; margin:0 auto; padding:40px 32px; }\n"
            + "  .header { background:linear-gradient(135deg,#7c3aed,#4f46e5); border-radius:20px;\n"
            + "            padding:36px 40px; display:flex; align-items:center; gap:28px; margin-bottom:32px; }\n"
            + "  .brand { font-size:28px; font-weight:800; color:#fff; }\n"
            + "  .brand span { color:#c4b5fd; }\n"
            + "  .user-name  { font-size:22px; font-weight:700; color:#fff; }\n"
            + "  .user-email { color:#c4b5fd; font-size:14px; margin-top:4px; }\n"
            + "  .badge { display:inline-block; background:rgba(255,255,255,0.2); border-radius:20px;\n"
            + "           padding:4px 14px; font-size:12px; color:#fff; margin-top:8px; }\n"
            + "  .section { background:#12172b; border:1px solid #1e2a45; border-radius:16px;\n"
            + "             padding:24px 28px; margin-bottom:24px; }\n"
            + "  .section-title { font-size:16px; font-weight:700; color:#a78bfa; margin-bottom:18px;\n"
            + "                   padding-bottom:10px; border-bottom:1px solid #1e2a45; }\n"
            + "  .info-grid { display:grid; grid-template-columns:1fr 1fr; gap:16px; }\n"
            + "  .info-item label { font-size:12px; color:#64748b; display:block; margin-bottom:4px; }\n"
            + "  .info-item .value { font-size:15px; color:#e2e8f0; font-weight:500; }\n"
            + "  .stats-grid { display:grid; grid-template-columns:repeat(2,1fr); gap:16px; }\n"
            + "  .stat-card { background:#0d1117; border:1px solid #1e2a45; border-radius:12px;\n"
            + "               padding:20px; text-align:center; }\n"
            + "  .stat-card .num { font-size:26px; font-weight:800; color:#7c3aed; }\n"
            + "  .stat-card .lbl { font-size:12px; color:#94a3b8; margin-top:6px; }\n"
            + "  .footer { text-align:center; color:#475569; font-size:12px; margin-top:40px;\n"
            + "            padding-top:20px; border-top:1px solid #1e2a45; }\n"
            + "</style>\n"
            + "</head>\n"
            + "<body>\n"
            + "<div class='page'>\n"

            // ── En-tête ───────────────────────────────────────────────────
            + "  <div class='header'>\n"
            + "    <div style='font-size:48px;'>🎮</div>\n"
            + "    <div style='flex:1'>\n"
            + "      <div class='brand'>Gami<span>lha</span></div>\n"
            + "      <div style='color:rgba(255,255,255,0.6);font-size:12px;margin-top:2px;'>Profil Utilisateur</div>\n"
            + "    </div>\n"
            + "    <div style='text-align:right;'>\n"
            + "      <div class='user-name'>" + user.getName() + "</div>\n"
            + "      <div class='user-email'>" + user.getEmail() + "</div>\n"
            + "      <div class='badge'>" + role + "</div>\n"
            + "    </div>\n"
            + "  </div>\n"

            // ── Informations personnelles ─────────────────────────────────
            + "  <div class='section'>\n"
            + "    <div class='section-title'>📋 Informations personnelles</div>\n"
            + "    <div class='info-grid'>\n"
            + "      <div class='info-item'><label>Nom complet</label><div class='value'>" + user.getName() + "</div></div>\n"
            + "      <div class='info-item'><label>Email</label><div class='value'>" + user.getEmail() + "</div></div>\n"
            + "      <div class='info-item'><label>Rôle</label><div class='value'>" + role + "</div></div>\n"
            + "      <div class='info-item'><label>Statut</label><div class='value'>" + status + "</div></div>\n"
            + "      <div class='info-item'><label>Membre depuis</label><div class='value'>" + createdAt + "</div></div>\n"
            + "      <div class='info-item'><label>Dernière activité</label><div class='value'>" + lastSeen + "</div></div>\n"
            + "    </div>\n"
            + "  </div>\n"

            // ── Statistiques d'activité ───────────────────────────────────
            + "  <div class='section'>\n"
            + "    <div class='section-title'>📊 Statistiques d'activité</div>\n"
            + "    <div class='stats-grid'>\n"
            + "      <div class='stat-card'>\n"
            + "        <div class='num'>" + ActivitySession.formatDuration(totalSec) + "</div>\n"
            + "        <div class='lbl'>Temps total sur la plateforme</div>\n"
            + "      </div>\n"
            + "      <div class='stat-card'>\n"
            + "        <div class='num'>" + ActivitySession.formatDuration(todaySec) + "</div>\n"
            + "        <div class='lbl'>Temps passé aujourd'hui</div>\n"
            + "      </div>\n"
            + "      <div class='stat-card'>\n"
            + "        <div class='num'>" + totalSess + "</div>\n"
            + "        <div class='lbl'>Nombre total de sessions</div>\n"
            + "      </div>\n"
            + "      <div class='stat-card'>\n"
            + "        <div class='num'>" + ActivitySession.formatDuration(avgSec) + "</div>\n"
            + "        <div class='lbl'>Durée moyenne / session</div>\n"
            + "      </div>\n"
            + "    </div>\n"
            + "  </div>\n"

            // ── Pied de page ──────────────────────────────────────────────
            + "  <div class='footer'>\n"
            + "    Document généré automatiquement par <strong>Gamilha Platform</strong>"
            + " le " + now + " · Confidentiel — Usage personnel uniquement\n"
            + "  </div>\n"
            + "</div>\n"
            + "</body>\n"
            + "</html>\n";

        // ── Écriture du fichier ───────────────────────────────────────────
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
            pw.print(html);
            System.out.println("✅ Profil généré : " + filePath);
            return filePath;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getOutputDir() { return OUTPUT_DIR; }
}
