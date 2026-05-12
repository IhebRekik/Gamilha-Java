<div align="center">

```
  ██████╗  █████╗ ███╗   ███╗██╗██╗     ██╗  ██╗ █████╗ 
 ██╔════╝ ██╔══██╗████╗ ████║██║██║     ██║  ██║██╔══██╗
 ██║  ███╗███████║██╔████╔██║██║██║     ███████║███████║
 ██║   ██║██╔══██║██║╚██╔╝██║██║██║     ██╔══██║██╔══██║
 ╚██████╔╝██║  ██║██║ ╚═╝ ██║██║███████╗██║  ██║██║  ██║
  ╚═════╝ ╚═╝  ╚═╝╚═╝     ╚═╝╚═╝╚══════╝╚═╝  ╚═╝╚═╝  ╚═╝
```

### 🎮 All-in-One Gaming Platform

**Compete · Stream · Connect · Rise**

<br/>

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![JavaFX](https://img.shields.io/badge/JavaFX-17.0.8-0078D7?style=for-the-badge&logo=java&logoColor=white)](https://openjfx.io/)
[![PHP](https://img.shields.io/badge/PHP-8.2-777BB4?style=for-the-badge&logo=php&logoColor=white)](https://www.php.net/)
[![Symfony](https://img.shields.io/badge/Symfony-6.x-000000?style=for-the-badge&logo=symfony&logoColor=white)](https://symfony.com/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)

<br/>

[![Stripe](https://img.shields.io/badge/Stripe-Payments-635BFF?style=flat-square&logo=stripe&logoColor=white)](https://stripe.com/)
[![Ably](https://img.shields.io/badge/Ably-Realtime-FF5416?style=flat-square)](https://ably.com/)
[![Ollama](https://img.shields.io/badge/Ollama-AI%20Chat-black?style=flat-square)](https://ollama.com/)
[![Vosk](https://img.shields.io/badge/Vosk-Speech-green?style=flat-square)](https://alphacephei.com/vosk/)
[![ZXing](https://img.shields.io/badge/ZXing-QR%20Code-orange?style=flat-square)](https://github.com/zxing/zxing)
[![VLC](https://img.shields.io/badge/VLCJ-Media-FF8800?style=flat-square&logo=vlcmediaplayer&logoColor=white)](https://github.com/caprica/vlcj)
[![Tailwind](https://img.shields.io/badge/Tailwind-CSS-38B2AC?style=flat-square&logo=tailwindcss&logoColor=white)](https://tailwindcss.com/)
[![License](https://img.shields.io/badge/License-MIT-22c55e?style=flat-square)](LICENSE)

<br/>

> **Gamilha** est une plateforme gaming complète disponible en deux versions :  
> une **application desktop Java/JavaFX** et une **application web Symfony/PHP**.  
> Les deux partagent la même base de données MySQL et les mêmes intégrations tierces.

</div>

---

## 📋 Table des Matières

- [Description](#-description)
- [Architecture Physique](#-architecture-physique)
- [Fonctionnalités & Modules](#-fonctionnalités--modules)
- [Version Desktop — Java/JavaFX](#-version-desktop--javajavafx)
- [Version Web — Symfony/PHP](#-version-web--symfonyphp)
- [Base de Données Partagée](#-base-de-données-partagée)
- [Équipe & Modules](#-équipe--modules)
- [Topics & Mots-clés](#-topics--mots-clés)

---

## 📖 Description

**Gamilha** (de l'arabe *جميلة* — belle) est un projet universitaire développé à **ESPRIT**, conçu comme une plateforme gaming communautaire tout-en-un. Elle réunit dans un seul écosystème tout ce dont un gamer a besoin :

-  **Compétitions** : tournois, brackets, matchs, équipes
-  **Streaming** : streams en direct, donations temps réel, analytics, prédiction ML
-  **Social** : posts, commentaires, amis, messagerie privée
-  **Coaching** : vidéos, playlists, ressources par jeu
-  **Monétisation** : abonnements premium, paiements Stripe, QR codes
-  **IA** : assistant ChatAI (Ollama), avatars générés, reconnaissance vocale (Vosk)

Le projet existe en **deux implémentations complémentaires** partageant la même base de données :

| | Desktop | Web |
|---|---|---|
| **Langage** | Java 17 | PHP 8.2 |
| **Framework** | JavaFX 17 + FXML | Symfony 6 + Twig |
| **Public** | Gamers power-users | Tous publics, tout device |

---

## 🏗️ Architecture Physique

```
╔══════════════════════════════════════════════════════════════════════════════════╗
║                         GAMILHA — ARCHITECTURE PHYSIQUE                        ║
╚══════════════════════════════════════════════════════════════════════════════════╝

  ┌─────────────────────────────────────────────────────────────────────────────┐
  │                          POSTE UTILISATEUR / CLIENT                         │
  │                                                                             │
  │   ┌──────────────────────────────┐   ┌──────────────────────────────────┐  │
  │   │     APPLICATION DESKTOP      │   │         NAVIGATEUR WEB           │  │
  │   │       Java 17 + JavaFX       │   │    Chrome / Firefox / Edge       │  │
  │   │                              │   │                                  │  │
  │   │  ┌────────────────────────┐  │   │  ┌────────────────────────────┐  │  │
  │   │  │    Couche Vue (FXML)   │  │   │  │    Couche Vue (Twig/HTML)  │  │  │
  │   │  │  • Interfaces User     │  │   │  │  • Templates Symfony       │  │  │
  │   │  │  • Interfaces Admin    │  │   │  │  • Tailwind CSS            │  │  │
  │   │  │  • CSS JavaFX          │  │   │  │  • JavaScript ES6+         │  │  │
  │   │  └────────────┬───────────┘  │   │  └────────────┬───────────────┘  │  │
  │   │               │              │   │               │                  │  │
  │   │  ┌────────────▼───────────┐  │   │  ┌────────────▼───────────────┐  │  │
  │   │  │  Couche Contrôleurs    │  │   │  │   Couche Contrôleurs       │  │  │
  │   │  │  (MVC JavaFX)          │  │   │  │   (Symfony MVC)            │  │  │
  │   │  │  • StreamController    │  │   │  │  • StreamController.php    │  │  │
  │   │  │  • DonationController  │  │   │  │  • DonationController.php  │  │  │
  │   │  │  • AdminControllers    │  │   │  │  • AdminControllers.php    │  │  │
  │   │  │  • BadgeController     │  │   │  │  • SecurityController.php  │  │  │
  │   │  └────────────┬───────────┘  │   │  └────────────┬───────────────┘  │  │
  │   │               │              │   │               │                  │  │
  │   │  ┌────────────▼───────────┐  │   │  ┌────────────▼───────────────┐  │  │
  │   │  │   Couche Services      │  │   │  │   Couche Services          │  │  │
  │   │  │  • StreamService       │  │   │  │  • StreamService.php       │  │  │
  │   │  │  • DonationService     │  │   │  │  • DonationService.php     │  │  │
  │   │  │  • StreamAnalytics     │  │   │  │  • AblyService.php         │  │  │
  │   │  │  • StreamPrediction    │  │   │  │  • StripeService.php       │  │  │
  │   │  │  • AblyService         │  │   │  └────────────┬───────────────┘  │  │
  │   │  │  • BadgeService        │  │   │               │                  │  │
  │   │  │  • OllamaService       │  │   │  ┌────────────▼───────────────┐  │  │
  │   │  │  • UserService         │  │   │  │    Couche Repository       │  │  │
  │   │  └────────────┬───────────┘  │   │  │    Doctrine ORM            │  │  │
  │   │               │              │   │  └────────────┬───────────────┘  │  │
  │   │  ┌────────────▼───────────┐  │   │               │                  │  │
  │   │  │  Couche Utilitaires    │  │   └───────────────┼──────────────────┘  │
  │   │  │  • AppConfig           │  │                   │                     │
  │   │  │  • SessionContext      │  │                   │                     │
  │   │  │  • QrCodeUtil (ZXing)  │  │                   │                     │
  │   │  │  • EmailSender (Mail)  │  │                   │                     │
  │   │  │  • DatabaseConnection  │  │                   │                     │
  │   │  └────────────┬───────────┘  │                   │                     │
  │   └───────────────┼──────────────┘                   │                     │
  │                   │                                  │                     │
  │   ┌───────────────▼──────────────────────────────────▼─────────────────┐  │
  │   │                    COMPOSANTS LOCAUX INSTALLÉS                      │  │
  │   │   ┌───────────┐  ┌───────────┐  ┌──────────────┐  ┌─────────────┐ │  │
  │   │   │ VLC Media │  │   Vosk    │  │   Webcam     │  │    SMTP     │ │  │
  │   │   │  Player   │  │  (Speech) │  │   Capture    │  │   Client    │ │  │
  │   │   │  (vlcj)   │  │  Local    │  │  (sarxos)    │  │ (Jakarta)   │ │  │
  │   │   └───────────┘  └───────────┘  └──────────────┘  └─────────────┘ │  │
  │   └────────────────────────────────────────────────────────────────────┘  │
  └─────────────────────────────────────────────────────────────────────────────┘
                │  JDBC / MySQL Connector         │  HTTP/HTTPS (Doctrine)
                │  HTTPS (REST APIs)              │  HTTPS (REST APIs)
                ▼                                 ▼
  ┌─────────────────────────────────────────────────────────────────────────────┐
  │                           SERVEUR LOCAL / LAN                               │
  │                                                                             │
  │   ┌──────────────────────────────────────────────────────────────────────┐ │
  │   │                     MySQL Server 8.0                                 │ │
  │   │                  Base de données partagée                            │ │
  │   │                                                                      │ │
  │   │  Tables :  users · streams · donations · posts · commentaires        │ │
  │   │            equipes · evenements · inscriptions · brackets            │ │
  │   │            game_matches · abonnements · coaching_videos              │ │
  │   │            playlists · chat_messages · historique_paiement           │ │
  │   │            badges · password_reset_tokens                            │ │
  │   └──────────────────────────────────────────────────────────────────────┘ │
  │                                                                             │
  │   ┌──────────────────────────────────────────────────────────────────────┐ │
  │   │                   Ollama — LLM Local                                 │ │
  │   │               http://localhost:11434                                 │ │
  │   │            Modèle : llama3 / mistral (ChatAI + Avatar)              │ │
  │   └──────────────────────────────────────────────────────────────────────┘ │
  │                                                                             │
  │   ┌──────────────────────────────────────────────────────────────────────┐ │
  │   │               Serveur Web Symfony (Dev)                              │ │
  │   │                  symfony serve / Apache                              │ │
  │   │                  http://localhost:8000                               │ │
  │   └──────────────────────────────────────────────────────────────────────┘ │
  └─────────────────────────────────────────────────────────────────────────────┘
                │  HTTPS / WSS (WebSocket Secure)
                ▼
  ┌─────────────────────────────────────────────────────────────────────────────┐
  │                          SERVICES CLOUD EXTERNES                            │
  │                                                                             │
  │   ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────────────┐ │
  │   │   ABLY.IO       │   │   STRIPE API    │   │     SMTP / EMAIL        │ │
  │   │                 │   │                 │   │                         │ │
  │   │ WebSocket temps │   │ Paiements       │   │ Réinitialisation MDP    │ │
  │   │ réel :          │   │ sécurisés :     │   │ Notifications           │ │
  │   │ • Donations     │   │ • Checkout      │   │ Confirmations           │ │
  │   │   live          │   │ • Webhooks      │   │                         │ │
  │   │ • Streams new   │   │ • Abonnements   │   │ Jakarta Mail            │ │
  │   │ • Badges        │   │ • Historique    │   │ Symfony Mailer          │ │
  │   │                 │   │                 │   │                         │ │
  │   │ Canal :         │   │ SDK Java 24.x   │   │ Gmail SMTP              │ │
  │   │ streams:new     │   │ SDK PHP         │   │ Port 587 (TLS)          │ │
  │   │ donations:live  │   │                 │   │                         │ │
  │   └─────────────────┘   └─────────────────┘   └─────────────────────────┘ │
  │                                                                             │
  │   ┌─────────────────┐   ┌─────────────────┐                               │
  │   │  OPENROUTER AI  │   │   MAVEN CENTRAL │                               │
  │   │  (optionnel)    │   │   / JITPACK     │                               │
  │   │                 │   │                 │                               │
  │   │ Renforcement    │   │ Dépôts Maven :  │                               │
  │   │ prédictions ML  │   │ • emoji-java    │                               │
  │   │ streams via API │   │ • webcam-cap.   │                               │
  │   │ REST            │   │ • calendarfx    │                               │
  │   └─────────────────┘   └─────────────────┘                               │
  └─────────────────────────────────────────────────────────────────────────────┘
```

---

##  Fonctionnalités & Modules

###  Authentification & Sécurité
- Inscription, connexion, gestion des rôles (`ROLE_USER` / `ROLE_ADMIN`)
- Réinitialisation mot de passe par email (Jakarta Mail / Symfony Mailer)
- Verrouillage de compte, 2FA, audit log
- Capture webcam sur tentatives échouées *(Desktop)*
- Présence en ligne et dernière connexion

###  Tournois, Équipes & Matchs
- CRUD complet sur les **équipes**, **événements**, **inscriptions**
- **Brackets** de tournoi avec rendu visuel
- **Game Matches** : suivi des scores et classements
- Calendrier des participations (CalendarFX *(Desktop)*)

###  Streams & Donations 
- Création de streams (RTMP, clé stream, thumbnail, statut live)
- **Donations** avec réactions par paliers : 
- Mises à jour **temps réel via Ably** (WebSocket — canal `donations:live`)
- **Badges** débloqués selon les dons reçus
- **Prédiction ML** du pic de viewers (régression pondérée + OpenRouter optionnel)
- Dashboard analytics : viewers, revenus, tendances
- Modération admin : gestion streams & donations

###  Réseau Social
- Feed de posts avec emoji et images
- Commentaires et réactions
- Système d'amis, profils publics
- Messagerie privée temps réel

###  Coaching & Playlists
- Bibliothèque de vidéos coaching
- Lecteur VLCJ *(Desktop)* / HTML5 *(Web)*
- Playlists organisées par jeu et catégorie

###  Abonnements & Paiements
- Plans d'abonnement configurables
- Paiement sécurisé **Stripe** avec webhooks
- QR Code unique par abonnement (ZXing / endroid)
- Historique des transactions

###  Intelligence Artificielle
- **ChatAI** via Ollama (LLM local — llama3/mistral)
- Génération d'avatars IA
- Reconnaissance vocale **Vosk** *(Desktop)*
- Prédictions analytiques streams (régression + tendance)

###  Dashboard Admin
- KPIs globaux : users, revenus, streams actifs
- Gestion CRUD de tous les modules
- Statistiques donations & abonnements

---

##  Version Desktop — Java/JavaFX

### Tech Stack

| Catégorie | Technologie | Version |
|-----------|-------------|---------|
| Langage | Java | 17 |
| UI | JavaFX + FXML | 17.0.8 |
| Build | Apache Maven | 3.x |
| BDD | MySQL Connector/J | 8.3.0 |
| Temps réel | Ably Java SDK | 1.2.40 |
| Paiements | Stripe Java SDK | 24.10.0 |
| Lecteur media | VLCJ | 4.8.2 |
| IA LLM | Ollama (local) | — |
| Reconnaissance vocale | Vosk | 0.3.45 |
| QR Code | ZXing | 3.5.3 |
| Calendrier | CalendarFX | 11.12.6 |
| Email | Jakarta Mail | — |
| Sécurité mots de passe | jBCrypt | 0.4 |
| Webcam | Sarxos Webcam Capture | 0.3.12 |
| Emoji | emoji-java | 5.1.1 |
| JSON | Gson | 2.10.1 |
| Tests | JUnit 5 + Mockito | — |

### Structure

```
src/main/java/com/gamilha/
├── MainApp.java
├── controllers/
│   ├── admin/
│   │   ├── AdminStreamListController.java
│   │   ├── AdminStreamFormController.java
│   │   ├── AdminStreamPredictionController.java
│   │   ├── AdminDonationListController.java
│   │   ├── AdminDonationFormController.java
│   │   ├── AdminDonationStreamsController.java
│   │   ├── AdminAnalyticsController.java
│   │   └── AdminUsersController.java
│   ├── StreamFormController.java
│   ├── StreamListController.java
│   ├── StreamShowController.java
│   ├── DonationFormController.java
│   ├── DonationListController.java
│   ├── DonationShowController.java
│   ├── BadgeController.java
│   └── ...
├── entity/
│   ├── User.java                ├── Stream.java
│   ├── Donation.java            ├── Post.java
│   ├── Equipe.java              ├── Evenement.java
│   ├── Bracket.java             ├── GameMatch.java
│   ├── Abonnement.java          ├── CoachingVideo.java
│   ├── Playlist.java            ├── ChatMessage.java
│   └── HistoriquePaiement.java
├── services/
│   ├── StreamService.java
│   ├── DonationService.java
│   ├── StreamAnalyticsService.java
│   ├── StreamPredictionService.java
│   ├── AblyService.java
│   ├── BadgeService.java
│   ├── OllamaService.java
│   ├── UserService.java
│   └── ...
└── utils/
    ├── AppConfig.java           ├── SessionContext.java
    ├── ToastUtil.java           ├── QrCodeUtil.java
    ├── EmailSender.java         └── NavigationContext.java
```

### Installation Desktop

**Prérequis :** Java 17+, Maven 3.8+, MySQL 8.0, VLC installé

```bash
git clone https://github.com/your-username/gamilha-java.git
cd gamilha-java

# Configurer src/main/resources/com/gamilha/config.properties
mvn clean javafx:run
```

**`config.properties` :**
```properties
db.url=jdbc:mysql://localhost:3306/gamilha
db.username=root
db.password=

stripe.secret.key=sk_test_xxxx
ably.api.key=xxxx.xxxx:xxxx
mail.username=your@email.com
mail.password=yourpassword
ollama.url=http://localhost:11434
```

### Tests Desktop

```bash
mvn test
```

| Classe de test | Couverture |
|----------------|------------|
| `StreamPredictionServiceTest` | Algorithme ML prédiction |
| `StreamEntityBusinessTest` | Logique métier stream |
| `DonationTest` | Calculs & formatage donations |
| `PostServiceTest` | CRUD posts |
| `CommentaireServiceTest` | Logique commentaires |
| `UserTest` | Gestion utilisateurs |
| `AbonnementTest` | Plans & abonnements |
| `InscriptionTest` | Tournois & inscriptions |
| `SessionContextTest` | Gestion de session |
| `MediaHelperTest` | Utilitaire media |

---

##  Version Web — Symfony/PHP

### Tech Stack

| Catégorie | Technologie | Version |
|-----------|-------------|---------|
| Langage | PHP | 8.2 |
| Framework | Symfony | 6.x |
| Templates | Twig | 3.x |
| ORM | Doctrine | 2.x |
| CSS | Tailwind CSS | 3.x |
| JS | JavaScript ES6+ | — |
| BDD | MySQL | 8.0 |
| Temps réel | Ably JS SDK | — |
| Paiements | Stripe PHP SDK | — |
| Email | Symfony Mailer | — |
| QR Code | endroid/qr-code | — |
| Tests | PHPUnit | — |

### Structure

```
gamilha-web/
├── src/
│   ├── Controller/
│   │   ├── Admin/
│   │   │   ├── AdminStreamController.php
│   │   │   ├── AdminDonationController.php
│   │   │   ├── AdminAnalyticsController.php
│   │   │   └── AdminUserController.php
│   │   ├── StreamController.php
│   │   ├── DonationController.php
│   │   ├── PostController.php
│   │   ├── EquipeController.php
│   │   ├── AbonnementController.php
│   │   └── SecurityController.php
│   ├── Entity/
│   ├── Repository/
│   ├── Service/
│   │   ├── StreamService.php
│   │   ├── DonationService.php
│   │   ├── AblyService.php
│   │   └── StripeService.php
│   └── Form/
├── templates/
│   ├── admin/stream/
│   ├── admin/donation/
│   ├── stream/
│   ├── donation/
│   └── base.html.twig
├── migrations/
├── .env
└── composer.json
```

### Installation Web

**Prérequis :** PHP 8.2+, Composer, MySQL 8.0, Symfony CLI

```bash
git clone https://github.com/your-username/gamilha-web.git
cd gamilha-web

composer install
npm install && npm run build

cp .env .env.local
# Configurer .env.local

php bin/console doctrine:database:create
php bin/console doctrine:migrations:migrate

symfony serve
```

**`.env.local` :**
```env
DATABASE_URL="mysql://root:@127.0.0.1:3306/gamilha"
STRIPE_SECRET_KEY=sk_test_xxxx
STRIPE_WEBHOOK_SECRET=whsec_xxxx
ABLY_API_KEY=xxxx.xxxx:xxxx
MAILER_DSN=smtp://user:pass@smtp.gmail.com:587
APP_SECRET=your_secret_here
```

### API Endpoints principaux

| Méthode | Route | Description |
|---------|-------|-------------|
| `GET` | `/streams` | Liste des streams |
| `GET` | `/streams/{id}` | Détail d'un stream |
| `POST` | `/streams/new` | Créer un stream |
| `POST` | `/donations/new` | Enregistrer une donation |
| `GET` | `/donations/stream/{id}` | Donations par stream |
| `POST` | `/abonnements/checkout` | Initier paiement Stripe |
| `GET` | `/admin/analytics` | Dashboard analytics |
| `GET` | `/admin/streams` | Gestion admin streams |
| `GET` | `/admin/donations` | Gestion admin donations |

---

## 🗄️ Base de Données Partagée

```
users                   streams                 donations
├── id (PK)             ├── id (PK)             ├── id (PK)
├── name                ├── title               ├── amount
├── email               ├── description         ├── donor_name
├── password (BCrypt)   ├── game                ├── user_id  ──► users
├── roles (JSON)        ├── thumbnail           ├── stream_id ──► streams
├── is_active           ├── status              └── created_at
├── login_attempts      ├── stream_key
├── two_factor_enabled  ├── rtmp_server
├── is_online           ├── viewers
└── created_at          ├── is_live
                        ├── user_id  ──► users
equipes                 └── created_at          abonnements
├── id (PK)                                     ├── id (PK)
├── name                evenements              ├── type
├── logo                ├── id (PK)             ├── price
└── user_id  ──► users  ├── name                ├── qr_code
                        ├── date                ├── user_id ──► users
posts                   ├── lieu                └── created_at
├── id (PK)             └── equipe_id ──► equip.
├── content                                     historique_paiement
├── image               coaching_videos         ├── id (PK)
├── user_id  ──► users  ├── id (PK)             ├── montant
└── created_at          ├── title               ├── stripe_id
                        ├── url                 └── user_id ──► users
commentaires            └── playlist_id ──► pl.
├── id (PK)
├── content             badges
├── post_id  ──► posts  ├── id (PK)
└── user_id  ──► users  ├── name
                        ├── threshold
                        └── stream_id ──► streams
```

---

##  Équipe & Modules

| Module | Responsable | Desktop | Web |
|--------|-------------|:-------:|:---:|
| Authentification & Utilisateurs | — | 
| Posts & Commentaires | — | 
| Événements & Inscriptions | — | 
| Équipes & Brackets | — | 
| **Streams & Donations**  | — | 
| Coaching & Playlists | — |
| Abonnements & Paiements | — |
| Chat & Messagerie | — | 
| Dashboard Admin | — | 

---

##  Topics & Mots-clés

`java` · `javafx` · `php` · `symfony` · `twig` · `doctrine` · `mysql` · `maven` · `mvc` · `desktop-app` · `web-application` · `gaming-platform` · `esport` · `live-streaming` · `donations` · `realtime` · `ably` · `websocket` · `stripe` · `payments` · `tournament` · `bracket` · `coaching` · `vlcj` · `media-player` · `ai-chat` · `ollama` · `llm` · `speech-recognition` · `vosk` · `qr-code` · `zxing` · `tailwindcss` · `bcrypt` · `machine-learning` · `stream-prediction` · `university-project` · `esprit` · `tunisia`

---

<div align="center">

Built with  at **ESPRIT University** — Tunisia · 2024/2025

* Play ·  Connect ·  Rise*

</div>
