package com.gamilha;

import com.gamilha.entity.*;
import com.gamilha.services.CommentaireServiceTest;
import com.gamilha.services.MediaHelperTest;
import com.gamilha.services.NavigationContextTest;
import com.gamilha.services.PostServiceTest;
import com.gamilha.services.SessionContextTest;
import com.gamilha.services.StreamPredictionServiceTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Suite de tests Gamilha — complète.
 *
 * Lance tous les tests :  mvn test
 * Depuis IntelliJ       : clic droit → Run 'GamilhaTestSuite'
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  ENTITÉS (logique métier pure, sans DB)                         │
 * │   • UserTest                  — getters/setters, isAdmin()      │
 * │   • PostTest                  — logique post                    │
 * │   • CommentaireTest           — logique commentaire             │
 * │   • StreamTest                — streamKey, statusBadge, isLive  │
 * │   • StreamEntityBusinessTest  — règles Symfony, api.video       │
 * │   • DonationTest              — emoji, getFormattedAmount()     │
 * │   • PlaylistTest, AbonnementTest, InscriptionTest               │
 * │   • ChatMessageTest, CoachingVideoTest                          │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  SERVICES (sans dépendance DB)                                  │
 * │   • SessionContextTest         — session utilisateur            │
 * │   • NavigationContextTest      — navigation navbar              │
 * │   • StreamPredictionServiceTest — prédiction + donation logique │
 * │   • MediaHelperTest            — helpers média                  │
 * │   • PostServiceTest            — logique post service           │
 * │   • CommentaireServiceTest     — logique commentaire service    │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * TOTAL estimé : ~120 tests
 */
@Suite
@SuiteDisplayName("Suite complète des tests Gamilha JavaFX")
@SelectClasses({
        // ── Entités ─────────────────────────────────────────────
        UserTest.class,
        PostTest.class,
        CommentaireTest.class,
        StreamTest.class,
        StreamEntityBusinessTest.class,
        DonationTest.class,
        PlaylistTest.class,
        AbonnementTest.class,
        InscriptionTest.class,
        ChatMessageTest.class,
        CoachingVideoTest.class,

        // ── Services ────────────────────────────────────────────
        SessionContextTest.class,
        NavigationContextTest.class,
        StreamPredictionServiceTest.class,
        MediaHelperTest.class,
        PostServiceTest.class,
        CommentaireServiceTest.class
})
public class GamilhaTestSuite {
    // Classe vide — JUnit 5 gère tout via les annotations @Suite
}
