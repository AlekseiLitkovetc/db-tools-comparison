-- adjacency list hierarchy. Заполнить таблицу можно из AdjacencyListFirstInsert

create table adjacency_list_hierarchy
(
    id        integer primary key,
    rank      varchar,
    name      varchar unique,
    parent_id integer references adjacency_list_hierarchy (id)
);

create index on adjacency_list_hierarchy (parent_id);


-- adjacency list hierarchy read hierarchy
WITH RECURSIVE subordinates AS (SELECT *
                                FROM adjacency_list_hierarchy
                                WHERE id = 1
                                UNION
                                SELECT t.*
                                FROM adjacency_list_hierarchy t
                                         INNER JOIN subordinates s ON s.id = t.parent_id)
SELECT *
FROM subordinates
order by id;


-- adjacency list hierarchy read hierarchy with count
WITH RECURSIVE subordinates AS (SELECT *
                                FROM adjacency_list_hierarchy
                                WHERE id = 1
                                UNION
                                SELECT t.*
                                FROM adjacency_list_hierarchy t
                                         INNER JOIN subordinates s ON s.id = t.parent_id)
SELECT *,
       (WITH RECURSIVE subordinates AS (SELECT id
                                        FROM adjacency_list_hierarchy
                                        WHERE id = subordinates.id
                                        UNION
                                        SELECT t.id
                                        FROM adjacency_list_hierarchy t
                                                 INNER JOIN subordinates s ON s.id = t.parent_id)
        SELECT count(*)
        FROM subordinates) as cnt
from subordinates;
