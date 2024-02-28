-- ltree
create extension if not exists ltree;

create table ltree_hierarchy
(
    id   integer unique,
    rank varchar,
    name varchar unique,
    path ltree
);


CREATE INDEX path_gist_idx ON ltree_hierarchy USING gist (path);
CREATE INDEX path_idx ON ltree_hierarchy USING btree (path);

-- заполним из adjacency_list_hierarchy
insert into ltree_hierarchy
WITH RECURSIVE subordinates AS (SELECT *, 1 as level, name as sort_path
                                FROM adjacency_list_hierarchy
                                WHERE id = 1
                                UNION
                                SELECT t.*, level + 1, sort_path || '.' || t.name
                                FROM adjacency_list_hierarchy t
                                         INNER JOIN subordinates s ON s.id = t.parent_id)
SELECT id, rank, name, sort_path::ltree
from subordinates;


-- ltree examples
select *
from hierarchy.ltree_hierarchy
where path ~ '*.defenseMinister.*';

-- только прямых подчиненных
select *
from hierarchy.ltree_hierarchy
where path ~ '*.defenseMinister.*{1}';

-- заменим colonelGeneral_1 на colonelGeneral_228 у всех path, где есть colonelGeneral_1
update hierarchy.ltree_hierarchy
set path = subpath('defenseMinister.colonelGeneral_1', 0, -1) ||
           'colonelGeneral_228':: ltree ||
           subpath(path, nlevel('defenseMinister.colonelGeneral_1'))
where path ~ 'defenseMinister.colonelGeneral_1.*{1,}';



-- касты для String <-> ltree
create
    or replace function ltree_invarchar(varchar) returns ltree as
$$
select ltree_in($1::cstring);
$$
    language sql immutable;

do
$$
    begin
        create cast (varchar as ltree) with function ltree_invarchar(varchar) as implicit;
    exception
        when duplicate_object then null;
    end
$$;