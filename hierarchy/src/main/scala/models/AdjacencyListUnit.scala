package models

final case class AdjacencyListUnit(
    id: Int,
    rank: String,
    name: String,
    parentId: Option[Int]
)
