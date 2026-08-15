create table song_scrape_request (
    id bigint not null auto_increment,
    title varchar(255) not null,
    singer varchar(255) not null,
    categories json,
    release_date date not null,
    play_seconds integer not null,
    answers varchar(255),
    hint varchar(255),
    created_at datetime(6) not null,
    primary key (id),
    constraint uk_song_scrape_request_singer_title unique (singer, title),
    constraint uk_song_scrape_request_title_date unique (title, release_date)
) engine = InnoDB;
