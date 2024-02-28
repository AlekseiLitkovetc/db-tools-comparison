package models

final case class LtreeUnitLtree(
    id: Int,
    rank: String,
    name: String,
    path: skunk.data.LTree
)
