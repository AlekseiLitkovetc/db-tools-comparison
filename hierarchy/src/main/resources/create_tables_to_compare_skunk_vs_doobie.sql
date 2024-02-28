-- создадим таблицы,которые используются в AdjacencyListInsert

create table hierarchy.adjacency_list_hierarchy_doobie
(
    id        integer primary key,
    rank      varchar,
    name      varchar,
    parent_id integer references hierarchy.adjacency_list_hierarchy_doobie (id)
);

create index on hierarchy.adjacency_list_hierarchy_doobie (parent_id);


create table hierarchy.adjacency_list_hierarchy_skunk
(
    id        integer primary key,
    rank      varchar,
    name      varchar,
    parent_id integer references hierarchy.adjacency_list_hierarchy_skunk (id)
);

create index on hierarchy.adjacency_list_hierarchy_skunk (parent_id);


-- создадим таблицы,которые используются в AdjacencyListUpdate

create table hierarchy.ltree_hierarchy_skunk
(
    id   integer unique,
    rank varchar,
    name varchar unique,
    path ltree
);

create index on hierarchy.ltree_hierarchy_skunk using gist (path);
create index on hierarchy.ltree_hierarchy_skunk using gist (path);

create table hierarchy.ltree_hierarchy_doobie
(
    id   integer unique,
    rank varchar,
    name varchar unique,
    path ltree
);

create index on hierarchy.ltree_hierarchy_doobie using gist (path);
create index on hierarchy.ltree_hierarchy_doobie using gist (path);