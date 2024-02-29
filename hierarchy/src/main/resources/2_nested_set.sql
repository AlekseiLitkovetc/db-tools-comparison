-- nested set
create table nested_set_hierarchy
(
    id         integer primary key,
    rank       varchar,
    name       varchar unique,
    parent_id  integer references nested_set_hierarchy (id),
    level      integer,
    lt         integer,
    rt         integer,
    node_count integer
);

create index on nested_set_hierarchy (parent_id);


--nested set from adjacency list hierarchy, about 55 min on Ryzen 7
insert into nested_set_hierarchy (id, rank, name, parent_id, level, lt, rt, node_count)
select id,
       rank,
       name,
       parent_id,
       height,
       traversal_id * 2 - height                            as lt,
       (node_cnt - 1) * 2 + (traversal_id * 2 - height) + 1 as rt,
       node_cnt
from (WITH RECURSIVE subordinates AS (SELECT *, 1 as height
                                      FROM adjacency_list_hierarchy
                                      WHERE id = 1
                                      UNION
                                      SELECT t.*, height + 1
                                      FROM adjacency_list_hierarchy t
                                               INNER JOIN subordinates s ON s.id = t.parent_id)
      SELECT row_number() over () as traversal_id,
             *,
             (WITH RECURSIVE subordinates AS (SELECT id
                                              FROM adjacency_list_hierarchy
                                              WHERE id = subordinates.id
                                              UNION
                                              SELECT t.id
                                              FROM adjacency_list_hierarchy t
                                                       INNER JOIN subordinates s ON s.id = t.parent_id)
              SELECT count(*)
              FROM subordinates)  as node_cnt
      FROM subordinates) adj_list;


--adj list with some fields https://www.sqlservercentral.com/articles/hierarchies-on-steroids-1-convert-an-adjacency-list-to-nested-sets
-- ниже просто способ создавать nested set из adjacency list быстрее
create table adjacency_list_hierarchy_busted
(
    id        integer primary key,
    rank      varchar,
    name      varchar unique,
    parent_id integer references adjacency_list_hierarchy_busted (id),
    level     integer,
    sort_path varchar
);
create index on adjacency_list_hierarchy_busted (parent_id);
create index on adjacency_list_hierarchy_busted (sort_path);


insert into adjacency_list_hierarchy_busted
WITH RECURSIVE subordinates AS (SELECT *, 1 as level, lpad(id::varchar, 7, '0') as sort_path
                                FROM adjacency_list_hierarchy
                                WHERE id = 1
                                UNION
                                SELECT t.*, level + 1, sort_path || lpad(t.id::varchar, 7, '0')
                                FROM adjacency_list_hierarchy t
                                         INNER JOIN subordinates s ON s.id = t.parent_id)
SELECT *
from subordinates;


-- one more nested set table for competition
create table nested_set_hierarchy_from_busted
(
    id         integer primary key,
    rank       varchar,
    name       varchar unique,
    parent_id  integer references nested_set_hierarchy_from_busted (id),
    level      integer,
    lt         integer,
    rt         integer,
    node_count integer
);

create index on nested_set_hierarchy_from_busted (parent_id);


-- for quick adj list to nested set
-- тут есть магические константы: num * 7 + 1 - так как у нас максимальный id это 1000000 - в нем 7 разрядов. и (0, 9) - так как у нас 10 уровней иерархии
create table levels
(
    n integer
);
insert into levels
SELECT num * 7 + 1
FROM generate_series(0, 9) num;


-- filled in ~17 sec
insert into nested_set_hierarchy_from_busted (id, rank, name, parent_id, level, lt, rt, node_count)
select id,
       rank,
       name,
       parent_id,
       level,
       traversal_id * 2 - level                            as lt,
       (node_cnt - 1) * 2 + (traversal_id * 2 - level) + 1 as rt,
       node_cnt
from (WITH RECURSIVE subordinates AS (SELECT *
                                      FROM adjacency_list_hierarchy_busted
                                      WHERE id = 1
                                      UNION
                                      SELECT t.*
                                      FROM adjacency_list_hierarchy_busted t
                                               INNER JOIN subordinates s ON s.id = t.parent_id)
      SELECT row_number() over () as traversal_id,
             *
      FROM subordinates) adj_list
         join (select SUBSTRING(h.sort_path, l.n, 7)::int as eid, count(*) as node_cnt
               from adjacency_list_hierarchy_busted h,
                    levels l
               where 1 <= l.n
                 and l.n <= length(sort_path)
               group by SUBSTRING(h.sort_path, l.n, 7)) coint_table on coint_table.eid = adj_list.id;
