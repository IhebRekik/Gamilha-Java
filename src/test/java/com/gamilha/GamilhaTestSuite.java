package com.gamilha;

import com.gamilha.entity.CommentaireTest;
import com.gamilha.entity.PostTest;
import com.gamilha.entity.UserTest;
import com.gamilha.services.CommentaireServiceTest;
import com.gamilha.services.MediaHelperTest;
import com.gamilha.services.PostServiceTest;
import com.gamilha.services.SessionContextTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Suite de tests Gamilha.
 *
 * Lance TOUS les tests du projet en une seule commande :
 *   mvn test
 *
 * Ou depuis IntelliJ : clic droit → Run 'GamilhaTestSuite'
 *
 * Organisation :
 *  ┌─────────────────────────────────────────────────────────────┐
 *  │  MODÈLES (aucune dépendance externe)                        │
 *  │   • UserTest          — 14 tests                            │
 *  │   • PostTest          — 10 tests                            │
 *  │   • CommentaireTest   — 12 tests                            │
 *  ├─────────────────────────────────────────────────────────────┤
 *  │  SERVICES (avec Mockito pour simuler la BD)                 │
 *  │   • SessionContextTest    —  9 tests                        │
 *  │   • MediaHelperTest       — 14 tests                        │
 *  │   • PostServiceTest       — 10 tests                        │
 *  │   • CommentaireServiceTest— 10 tests                        │
 *  └─────────────────────────────────────────────────────────────┘
 *
 * TOTAL : ~79 tests
 */
@Suite
@SuiteDisplayName("Suite complète des tests Gamilha JavaFX")
@SelectClasses({
    // ── Modèles ──────────────────────────────────────────
    UserTest.class,
    PostTest.class,
    CommentaireTest.class,
    // ── Services ─────────────────────────────────────────
    SessionContextTest.class,
    MediaHelperTest.class,
    PostServiceTest.class,
    CommentaireServiceTest.class
})
public class GamilhaTestSuite {
    // Classe vide — JUnit 5 s'occupe de tout via les annotations
}
