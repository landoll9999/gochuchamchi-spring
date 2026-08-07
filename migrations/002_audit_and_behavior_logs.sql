-- 기존 운영 DB에 한 번 적용하는 마이그레이션이다.
-- 신규 로컬 DB는 루트 schema.sql에서 같은 테이블을 생성한다.

CREATE TABLE IF NOT EXISTS audit_logs (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  event_type varchar(64) NOT NULL,
  outcome varchar(16) NOT NULL,
  actor_user_id bigint(20) DEFAULT NULL,
  actor_username varchar(50) DEFAULT NULL,
  target_type varchar(32) DEFAULT NULL,
  target_id varchar(100) DEFAULT NULL,
  request_method varchar(10) DEFAULT NULL,
  request_path varchar(255) DEFAULT NULL,
  ip_address varchar(45) DEFAULT NULL,
  user_agent varchar(500) DEFAULT NULL,
  reason_code varchar(64) DEFAULT NULL,
  details varchar(1000) DEFAULT NULL,
  occurred_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_audit_occurred_at (occurred_at),
  KEY idx_audit_actor (actor_user_id, occurred_at),
  KEY idx_audit_event (event_type, outcome, occurred_at),
  KEY idx_audit_target (target_type, target_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_behavior_logs (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  event_type varchar(64) NOT NULL,
  user_id bigint(20) DEFAULT NULL,
  anonymous_id char(36) NOT NULL,
  behavior_session_id char(36) NOT NULL,
  request_path varchar(255) NOT NULL,
  resource_type varchar(32) DEFAULT NULL,
  resource_id varchar(100) DEFAULT NULL,
  metadata varchar(1000) DEFAULT NULL,
  response_status smallint NOT NULL,
  occurred_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_behavior_occurred_at (occurred_at),
  KEY idx_behavior_user (user_id, occurred_at),
  KEY idx_behavior_anonymous (anonymous_id, occurred_at),
  KEY idx_behavior_session (behavior_session_id, occurred_at),
  KEY idx_behavior_event (event_type, occurred_at),
  KEY idx_behavior_resource (resource_type, resource_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
