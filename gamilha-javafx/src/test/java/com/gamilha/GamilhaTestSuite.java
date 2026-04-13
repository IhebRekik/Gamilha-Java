package com.gamilha;

import com.gamilha.model.CommentaireTest;
import com.gamilha.model.PostTest;
import com.gamilha.model.UserTest;
import com.gamilha.service.CommentaireServiceTest;
import com.gamilha.service.MediaHelperTest;
import com.gamilha.service.PostServiceTest;
import com.gamilha.service.SessionContextTest;
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
