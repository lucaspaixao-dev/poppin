package io.github.lucaspaixaodev.poppin.infrastructure.output.database.neo4j.user

import org.springframework.data.neo4j.repository.Neo4jRepository

interface UserNeo4jRepository : Neo4jRepository<UserNode, String>
