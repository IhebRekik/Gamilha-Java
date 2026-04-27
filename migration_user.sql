-- ============================================================
-- Migration SQL - Gamilha Enhanced User Management
-- Exécuter UNE SEULE FOIS sur votre base "gamylha"
-- ============================================================

USE gamylha;

-- Nouvelles colonnes pour la sécurité et présence
ALTER TABLE user
    ADD COLUMN IF NOT EXISTS login_attempts  INT          DEFAULT 0         COMMENT 'Nb tentatives ratées',
    ADD COLUMN IF NOT EXISTS lock_expiry     DATETIME     DEFAULT NULL      COMMENT 'Fin du blocage temporaire',
    ADD COLUMN IF NOT EXISTS last_seen       DATETIME     DEFAULT NULL      COMMENT 'Dernière activité',
    ADD COLUMN IF NOT EXISTS is_online       TINYINT(1)   DEFAULT 0         COMMENT '1=en ligne, 0=hors ligne',
    ADD COLUMN IF NOT EXISTS last_ip         VARCHAR(45)  DEFAULT NULL      COMMENT 'Dernière IP de connexion',
    ADD COLUMN IF NOT EXISTS last_device     VARCHAR(255) DEFAULT NULL      COMMENT 'Dernier appareil utilisé',
    ADD COLUMN IF NOT EXISTS two_factor_code VARCHAR(10)  DEFAULT NULL      COMMENT 'Code OTP 2FA',
    ADD COLUMN IF NOT EXISTS audit_log       MEDIUMTEXT   DEFAULT NULL      COMMENT 'Journal des actions',
    ADD COLUMN IF NOT EXISTS verified        TINYINT(1)   DEFAULT 0         COMMENT 'Compte vérifié';

-- S'assurer que ban_until a le bon type
ALTER TABLE user
    MODIFY COLUMN ban_until VARCHAR(30) DEFAULT NULL;

-- Initialiser last_seen pour les users existants
UPDATE user SET last_seen = created_at WHERE last_seen IS NULL;

SELECT 'Migration terminée avec succès !' AS statut;
