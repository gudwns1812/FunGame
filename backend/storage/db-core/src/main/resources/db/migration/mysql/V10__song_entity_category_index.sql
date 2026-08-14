create index idx_category_release_date
    on song_entity ((cast(categories as char(32) array)), release_date);
