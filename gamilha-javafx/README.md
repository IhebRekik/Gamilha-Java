# 📘 Documentation Complète — Gamilha JavaFX

Projet JavaFX qui reproduit la partie **Social** (Posts + Commentaires) du projet
Symfony Gamilha. Cette documentation explique **chaque couche, chaque fichier et
chaque décision de conception**.

---

## 🗂️ Arborescence complète du projet

```
gamilha-javafx/
│
├── pom.xml                              ← Configuration Maven (dépendances + plugins)
│
├── src/
│   ├── main/
│   │   ├── java/com/gamilha/
│   │   │   │
│   │   │   ├── MainApp.java             ← Point d'entrée de l'application
│   │   │   │
│   │   │   ├── model/                   ← Entités (miroir des entités Symfony)
│   │   │   │   ├── User.java
│   │   │   │   ├── Post.java
│   │   │   │   └── Commentaire.java
│   │   │   │
│   │   │   ├── service/                 ← Accès BD + logique métier
│   │   │   │   ├── DBConnection.java
│   │   │   │   ├── SessionContext.java
│   │   │   │   ├── MediaHelper.java
│   │   │   │   ├── UserService.java
│   │   │   │   ├── PostService.java
│   │   │   │   ├── CommentaireService.java
│   │   │   │   └── FriendService.java
│   │   │   │
│   │   │   └── controller/              ← Contrôleurs JavaFX (liés aux FXML)
│   │   │       ├── NavBarUserController.java
│   │   │       ├── NavBarAdminController.java
│   │   │       ├── UserPostController.java
│   │   │       ├── UserPostFormController.java
│   │   │       ├── UserAmisController.java
│   │   │       ├── UserMesPostsController.java
│   │   │       ├── UserMesCommentairesController.java
│   │   │       ├── UserCommentaireFormController.java
│   │   │       ├── AdminPostController.java
│   │   │       ├── AdminCommentaireController.java
│   │   │       ├── PostFormController.java
│   │   │       └── CommentaireFormController.java
│   │   │
│   │   └── resources/com/gamilha/
│   │       ├── styles.css               ← Thème sombre global
│   │       ├── images/
│   │       │   └── logo.png             ← Logo Gamilha
│   │       └── interfaces/
│   │           ├── User/                ← Interfaces côté utilisateur
│   │           │   ├── NavBarUser.fxml
│   │           │   ├── UserPostView.fxml
│   │           │   ├── UserPostFormView.fxml
│   │           │   ├── UserAmisView.fxml
│   │           │   ├── UserMesPostsView.fxml
│   │           │   ├── UserMesCommentairesView.fxml
│   │           │   └── UserCommentaireFormView.fxml
│   │           └── Admin/               ← Interfaces côté admin
│   │               ├── NavBarAdmin.fxml
│   │               ├── AdminPostView.fxml
│   │               ├── AdminCommentaireView.fxml
│   │               ├── PostFormView.fxml
│   │               └── CommentaireFormView.fxml
│   │
│   └── test/java/com/gamilha/           ← Tests unitaires
│       ├── GamilhaTestSuite.java
│       ├── model/
│       │   ├── UserTest.java
│       │   ├── PostTest.java
│       │   └── CommentaireTest.java
│       └── service/
│           ├── SessionContextTest.java
│           ├── MediaHelperTest.java
│           ├── PostServiceTest.java
│           └── CommentaireServiceTest.java
```

---

## 🧱 COUCHE 1 — Modèles (`model/`)

Les modèles sont la **représentation Java** des entités Symfony.

### `User.java`
**Miroir de** `src/Entity/User.php`

```java
private int     id;           // #[ORM\Id]
private String  name;         // #[ORM\Column]
private String  email;        // #[ORM\Column]
private String  profileImage; // #[ORM\Column(nullable: true)]
private String  roles;        // #[ORM\Column(type: 'json')]  → stocké en JSON
private boolean isActive;     // #[ORM\Column(type: 'boolean')]
private String  banUntil;     // #[ORM\Column(nullable: true)]
```

**Méthode clé : `isAdmin()`**
```java
public boolean isAdmin() {
    return roles != null && roles.contains("ROLE_ADMIN");
}
```
Reproduit en Java la logique Symfony `is_granted('ROLE_ADMIN')`.
Le champ `roles` est stocké en JSON dans MySQL : `["ROLE_USER"]` ou `["ROLE_ADMIN"]`.

---

### `Post.java`
**Miroir de** `src/Entity/Post.php`

```java
private int           id;
private String        content;     // #[Assert\Length(min: 12)]
private String        image;       // nom du fichier dans public/uploads/
private String        mediaurl;    // URL YouTube ou image distante
private LocalDateTime createdAt;
private User          user;        // #[ORM\ManyToOne] → jointure SQL JOIN
private int           likesCount;  // calculé via COUNT(post_likes)
private List<Commentaire> commentaires; // #[ORM\OneToMany]
```

La liste `commentaires` est initialisée à `new ArrayList<>()` dans le constructeur
vide pour éviter les `NullPointerException`.

---

### `Commentaire.java`
**Miroir de** `src/Entity/Commentaire.php`

```java
private int           id;
private String        text;     // #[Assert\Length(min:5, max:500)]
private LocalDateTime createdAt;
private Post          post;     // #[ORM\ManyToOne] → ManyToOne vers Post
private User          user;     // #[ORM\ManyToOne] → ManyToOne vers User
```

**Relations** :
- Un commentaire appartient à **un** post (`ManyToOne`)
- Un commentaire est écrit par **un** utilisateur (`ManyToOne`)

---

## 🗄️ COUCHE 2 — Services (`service/`)

Les services sont la **couche d'accès à la base de données**.
Ils remplacent les Repository Symfony et les Controller Symfony en un seul endroit.

### `DBConnection.java`
```java
// Singleton : une seule instance de connexion
private static Connection instance;

public static Connection getInstance() {
    if (instance == null || instance.isClosed()) {
        instance = DriverManager.getConnection(URL, USER, PASSWORD);
    }
    return instance;
}
```
**Pourquoi un Singleton ?** Pour ne pas ouvrir 10 connexions MySQL simultanées.
La connexion est réutilisée dans tous les services.

⚠️ **À configurer** : `URL`, `USER`, `PASSWORD` selon ton `.env` Symfony.

---

### `PostService.java`
Contient les 5 opérations CRUD + la recherche.

**Requête `findAll()` avec JOIN** :
```sql
SELECT p.id, p.content, p.image, p.created_at, p.mediaurl, p.user_id,
       u.name AS u_name, u.email AS u_email, u.profile_image AS u_pic,
       u.roles, u.is_active, u.ban_until,
       (SELECT COUNT(*) FROM post_likes pl WHERE pl.post_id = p.id) AS likes_count
FROM post p
JOIN `user` u ON u.id = p.user_id
ORDER BY p.id DESC
```

Cette requête charge le Post ET son User en **une seule requête SQL**
(évite le problème N+1 qu'on a en Symfony si on ne fait pas `JOIN`).

**Méthode `map()`** : convertit une ligne SQL (`ResultSet`) en objet Java (`Post`).

---

### `CommentaireService.java`
Deux variantes de mapping :
- `mapLight()` : juste l'ID du post (pour `findByPost()`)
- `mapFull()` : avec les infos du post (pour `findAll()` admin)

---

### `MediaHelper.java`
**Reproduit la logique Twig** de `social/index.html.twig` :

```twig
{% if 'youtube.com' in post.mediaurl %}
    → embed YouTube
{% elseif post.mediaurl starts with 'http' %}
    → afficher comme image
{% else %}
    → lien générique
{% endif %}
```

En Java :
```java
public static MediaType detect(String url) {
    if (url.contains("youtube.com") || url.contains("youtu.be")) return YOUTUBE;
    if (url.toLowerCase().endsWith(".jpg") || ...) return IMAGE_URL;
    if (url.startsWith("http")) return IMAGE_URL;
    return LINK;
}
```

Pour YouTube, `WebView` charge directement l'URL embed :
`https://www.youtube.com/embed/{videoId}`

---

### `SessionContext.java`
**Équivalent de `app.user` dans Twig.**
```java
// Symfony :  app.user
// JavaFX  :  SessionContext.getCurrentUser()

public static void setCurrentUser(User user) { currentUser = user; }
public static User getCurrentUser()          { return currentUser; }
public static void clear()                   { currentUser = null; }  // logout
public static boolean isLoggedIn()           { return currentUser != null; }
```

---

### `FriendService.java`
Gère la table `friend` de Symfony.
- `findFriends(userId)` → liste des amis
- `findSuggestions(userId, limit)` → utilisateurs pas encore amis
- `addFriend(userId, friendId)` → INSERT dans `friend`

---

## 🎮 COUCHE 3 — Contrôleurs (`controller/`)

Les contrôleurs sont le **lien entre les FXML (vues) et les services (données)**.
C'est l'équivalent des contrôleurs Symfony, mais pour JavaFX.

### `NavBarUserController.java`
**C'est le contrôleur principal.** Il gère la navigation entre les pages.

```java
@FXML public void goReseaux() {
    User u = SessionContext.getCurrentUser();
    if (u != null && u.isAdmin()) {
        // ROLE_ADMIN → cards admin (gestion de tous les posts)
        load(BASE_ADMIN + "AdminPostView.fxml");
    } else {
        // ROLE_USER → fil social (fil + sidebar amis)
        load(BASE_USER + "UserPostView.fxml");
    }
}
```

**Sous-menu Social** (masqué pour les admins) :
```
🏠 Fil d'actualité   → UserPostView.fxml
📝 Mes Posts         → UserMesPostsView.fxml
💬 Mes Commentaires  → UserMesCommentairesView.fxml
👤 Amis              → UserAmisView.fxml
```

---

### `UserPostController.java`
**Le plus complexe.** Reproduit `social/index.html.twig`.

**Construction dynamique des cards** (JavaFX n'a pas de moteur de template) :
```java
private VBox buildPostCard(Post post) {
    VBox card = new VBox(0);
    card.getChildren().addAll(
        buildPostHeader(post),    // avatar + nom + date + ⋮
        buildPostContent(post),   // texte + image locale
        buildPostMedia(post),     // YouTube WebView ou image URL
        buildPostStats(post),     // ❤ likes + 💬 commentaires
        sep(16),                  // séparateur
        buildCommentsList(post),  // liste des commentaires
        buildAddCommentRow(post)  // champ "Écrire un commentaire..."
    );
    return card;
}
```

**Bouton ⋮ (3 points)** — reproduit `{% if post.user == app.user %}` :
```java
// FXML Symfony :
// {% if post.user == app.user %}
//     <button data-bs-toggle="modal">...</button>
// {% endif %}

// JavaFX équivalent :
if (isOwner(post.getUser())) {
    Button menuBtn = menuButton();   // bouton ⋮
    menuBtn.setOnAction(e -> showPostMenu(menuBtn, post));
    header.getChildren().add(menuBtn);
}

private boolean isOwner(User u) {
    return currentUser != null && u != null && u.getId() == currentUser.getId();
}
```

**Menu contextuel** (équivalent du modal Bootstrap) :
```java
private void showPostMenu(Button anchor, Post post) {
    ContextMenu menu = new ContextMenu();
    MenuItem editItem = new MenuItem("✏ Modifier");
    MenuItem delItem  = new MenuItem("🗑 Supprimer");
    editItem.setOnAction(e -> openEditPost(post));
    delItem.setOnAction(e -> confirmDeletePost(post));
    menu.getItems().addAll(editItem, new SeparatorMenuItem(), delItem);
    menu.show(anchor, Side.BOTTOM, 0, 4);
}
```

---

### `AdminPostController.java`
Affiche **tous** les posts en cards (pas filtré par utilisateur).
Le bouton ⋮ est visible sur **chaque** post (pas de vérification de propriété).
Inclut un toggle "Voir commentaires" qui charge les commentaires du post inline.

---

## 🎨 COUCHE 4 — Interfaces FXML (`resources/interfaces/`)

Les fichiers `.fxml` sont l'équivalent des templates Twig.
Ils définissent la **structure visuelle** et les **liaisons avec les contrôleurs**.

### Syntaxe FXML
```xml
<!-- Lier la vue au contrôleur -->
fx:controller="com.gamilha.controller.UserPostController"

<!-- Lier un élément à une variable @FXML du contrôleur -->
<VBox fx:id="feedBox"/>

<!-- Lier un bouton à une méthode @FXML -->
<Button onAction="#onNewPost" text="Publier"/>
```

### Correction du TextArea (fond blanc → fond sombre)
Le problème venait de `-fx-control-inner-background` non défini.
Dans `styles.css` :
```css
.text-area .content {
    -fx-background-color: #1a1a30;
    -fx-control-inner-background: #1a1a30;  /* ← la clé */
}
```
Et dans chaque FXML :
```xml
<TextArea style="-fx-control-inner-background:#1a1a30;
                 -fx-text-fill:#e6edf3;
                 -fx-background-color:#1a1a30;"/>
```

### Logo dans la NavBar
```xml
<ImageView fitHeight="38" fitWidth="38" preserveRatio="true">
    <image>
        <!-- @ = chemin relatif dans le classpath (resources/) -->
        <Image url="@/com/gamilha/images/logo.png"/>
    </image>
</ImageView>
```

---

## 🧪 COUCHE 5 — Tests unitaires (`src/test/`)

### Technologies utilisées
| Outil | Rôle |
|---|---|
| **JUnit 5** | Framework de test principal |
| **Mockito** | Simuler la BD sans connexion réelle |
| `@Test` | Déclarer un test |
| `@BeforeEach` | Initialiser avant chaque test |
| `@AfterEach` | Nettoyer après chaque test |
| `@ParameterizedTest` | Exécuter avec plusieurs valeurs |
| `@Mock` | Créer un faux objet |
| `when().thenReturn()` | Définir le comportement du mock |
| `verify()` | Vérifier qu'une méthode a été appelée |

### Fichiers de tests

| Fichier | Ce qu'il teste | Complexité |
|---|---|---|
| `UserTest.java` | Modèle User : getters/setters, `isAdmin()` | ⭐ Simple |
| `PostTest.java` | Modèle Post : constructeurs, relations, dates | ⭐ Simple |
| `CommentaireTest.java` | Modèle Commentaire : validations Symfony | ⭐ Simple |
| `SessionContextTest.java` | Gestion de la session (login/logout) | ⭐⭐ Moyen |
| `MediaHelperTest.java` | Détection YouTube/image/lien | ⭐⭐ Moyen |
| `PostServiceTest.java` | Service Post avec Mockito BD | ⭐⭐⭐ Avancé |
| `CommentaireServiceTest.java` | Service Commentaire avec Mockito | ⭐⭐⭐ Avancé |

### Lancer les tests
```bash
# Tous les tests
mvn test

# Un fichier spécifique
mvn test -Dtest=UserTest

# Voir le rapport HTML
open target/surefire-reports/index.html
```

---

## 🚀 Lancement de l'application

```bash
# Modifier DBConnection.java : URL / USER / PASSWORD
# Modifier AdminPostController.java : UPLOADS = chemin vers public/uploads/

# Lancer en mode USER (ROLE_USER)
mvn clean javafx:run

# Pour basculer en ADMIN : dans MainApp.java,
# changer l'ID de l'utilisateur pour un ROLE_ADMIN
```

---

## 🔄 Correspondance Symfony ↔ JavaFX

| Symfony | JavaFX |
|---|---|
| `src/Entity/Post.php` | `model/Post.java` |
| `src/Repository/PostRepository.php` | `service/PostService.java` |
| `templates/social/index.html.twig` | `UserPostView.fxml` + `UserPostController.java` |
| `{% if post.user == app.user %}` | `isOwner(post.getUser())` |
| `app.user` | `SessionContext.getCurrentUser()` |
| `is_granted('ROLE_ADMIN')` | `user.isAdmin()` |
| `asset('uploads/' ~ post.image)` | `new File(UPLOADS + post.getImage())` |
| `MediaUrl → YouTube embed` | `MediaHelper.toEmbedUrl()` + `WebView` |
| Modal Bootstrap "Options" | `ContextMenu` JavaFX |
| `@Route('/social')` | `NavBarUserController.goReseaux()` |
| `KnpPaginator` | `FlowPane` + filtre Java en mémoire |
