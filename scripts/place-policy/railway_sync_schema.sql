SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'place' AND column_name = 'active'),
    'SELECT 1',
    'ALTER TABLE place ADD COLUMN active BIT NOT NULL DEFAULT 1'
);
PREPARE statement FROM @ddl; EXECUTE statement; DEALLOCATE PREPARE statement;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'place' AND column_name = 'pet_info_synced_modified_time'),
    'SELECT 1',
    'ALTER TABLE place ADD COLUMN pet_info_synced_modified_time VARCHAR(255) NULL'
);
PREPARE statement FROM @ddl; EXECUTE statement; DEALLOCATE PREPARE statement;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'place_pet_policy' AND column_name = 'review_pending'),
    'SELECT 1',
    'ALTER TABLE place_pet_policy ADD COLUMN review_pending BIT NOT NULL DEFAULT 0'
);
PREPARE statement FROM @ddl; EXECUTE statement; DEALLOCATE PREPARE statement;

CREATE TABLE IF NOT EXISTS place_pet_policy_review (
    id BIGINT NOT NULL AUTO_INCREMENT,
    place_id BIGINT NOT NULL,
    source_modified_time VARCHAR(255) NOT NULL,
    review_reasons TEXT NOT NULL,
    suggested_policy_json TEXT NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    resolved_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_policy_review_place_source_version UNIQUE (place_id, source_modified_time),
    CONSTRAINT fk_policy_review_place FOREIGN KEY (place_id) REFERENCES place (id)
);

UPDATE place p
JOIN place_pet_info ppi ON ppi.place_id = p.id
SET p.pet_info_synced_modified_time = COALESCE(p.modified_time, 'UNKNOWN')
WHERE p.pet_info_synced_modified_time IS NULL;
