-- 운영 DB 에만 한 번 직접 실행합니다. flyway 가 실행하지 않습니다.
--
-- V9, V10 이 만드는 제약과 인덱스는 운영 DB 에 이미 손으로 붙어 있습니다.
-- 그대로 두면 다음 배포에서 flyway 가 같은 제약을 또 만들려다 실패합니다.
-- 그래서 실행하지 않고 적용된 것으로만 기록합니다.
--
-- 실행 전에 아래 세 제약과 두 인덱스가 실제로 있는지 확인하세요.
-- 하나라도 없으면 이 스크립트를 실행하지 말고 그 제약을 먼저 만들어야 합니다.
--
--   SHOW INDEX FROM song_entity;
--
-- 기대: uq_title_date(title, release_date), singer(singer, title),
--       video_link(video_link), idx_answers(answers),
--       idx_category_release_date(cast(categories as char(32) array), release_date)

INSERT INTO flyway_schema_history
    (installed_rank, version, description, type, script, checksum,
     installed_by, installed_on, execution_time, success)
SELECT
    (SELECT MAX(installed_rank) FROM flyway_schema_history) + 1,
    '9',
    'song entity constraints',
    'SQL',
    'V9__song_entity_constraints.sql',
    -1474182248,
    CURRENT_USER(),
    NOW(),
    0,
    1
WHERE NOT EXISTS (
    SELECT 1 FROM (SELECT * FROM flyway_schema_history) h WHERE h.version = '9'
);

INSERT INTO flyway_schema_history
    (installed_rank, version, description, type, script, checksum,
     installed_by, installed_on, execution_time, success)
SELECT
    (SELECT MAX(installed_rank) FROM flyway_schema_history) + 1,
    '10',
    'song entity category index',
    'SQL',
    'mysql/V10__song_entity_category_index.sql',
    -404473388,
    CURRENT_USER(),
    NOW(),
    0,
    1
WHERE NOT EXISTS (
    SELECT 1 FROM (SELECT * FROM flyway_schema_history) h WHERE h.version = '10'
);

SELECT installed_rank, version, description, script, checksum, success
FROM flyway_schema_history
WHERE version IN ('9', '10');
