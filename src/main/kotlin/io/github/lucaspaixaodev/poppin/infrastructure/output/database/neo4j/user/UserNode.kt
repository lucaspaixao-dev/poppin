package io.github.lucaspaixaodev.poppin.infrastructure.output.database.neo4j.user

import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node

@Node("User")
class UserNode(
    @Id val id: String,
)
