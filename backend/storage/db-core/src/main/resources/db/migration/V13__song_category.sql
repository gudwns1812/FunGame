create table song_category (
    song_id bigint not null,
    category varchar(32) not null,
    primary key (song_id, category),
    constraint fk_song_category_song foreign key (song_id) references song_entity (id)
) engine = InnoDB;

create index idx_category_song on song_category (category, song_id);
