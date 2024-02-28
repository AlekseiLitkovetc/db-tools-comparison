-- создадим таблицы,которые используются в AdjacencyListInsert

create table adjacency_list_hierarchy_doobie
(
    id        integer primary key,
    rank      varchar,
    name      varchar unique,
    parent_id integer references adjacency_list_hierarchy_doobie (id)
);

create index on adjacency_list_hierarchy_doobie (parent_id);


create table adjacency_list_hierarchy_skunk
(
    id        integer primary key,
    rank      varchar,
    name      varchar unique,
    parent_id integer references adjacency_list_hierarchy_skunk (id)
);

create index on adjacency_list_hierarchy_skunk (parent_id);


-- создадим таблицы,которые используются в AdjacencyListUpdate

create table ltree_hierarchy_skunk
(
    id   integer primary key,
    rank varchar,
    name varchar unique,
    path ltree
);

create index on ltree_hierarchy_skunk using gist (path);
create index on ltree_hierarchy_skunk using btree (path);

create table ltree_hierarchy_doobie
(
    id   integer primary key,
    rank varchar,
    name varchar unique,
    path ltree
);

create index on ltree_hierarchy_doobie using gist (path);
create index on ltree_hierarchy_doobie using btree (path);
