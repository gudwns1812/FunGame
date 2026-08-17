insert into song_category (song_id, category)
select distinct song_entity.id, categories_as_rows.category
from song_entity,
     json_table(song_entity.categories, '$[*]'
                columns (category varchar(32) path '$')) as categories_as_rows
where song_entity.categories is not null;
