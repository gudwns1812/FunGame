alter table song_entity add unique (singer, title);
alter table song_entity add unique (video_link);
alter table song_entity add constraint uq_title_date unique (title, release_date);

create index idx_answers on song_entity (answers);
