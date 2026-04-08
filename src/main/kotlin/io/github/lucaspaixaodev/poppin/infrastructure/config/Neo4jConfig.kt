package io.github.lucaspaixaodev.poppin.infrastructure.config

import jakarta.persistence.EntityManagerFactory
import org.neo4j.driver.Driver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories
import org.springframework.orm.jpa.JpaTransactionManager

@Configuration
@EnableNeo4jRepositories(
    basePackages = ["io.github.lucaspaixaodev.poppin.infrastructure.output.database.neo4j"],
    transactionManagerRef = "neo4jTransactionManager"
)
class Neo4jConfig {

    @Bean
    @Primary
    fun transactionManager(entityManagerFactory: EntityManagerFactory): JpaTransactionManager {
        return JpaTransactionManager(entityManagerFactory)
    }

    @Bean
    fun neo4jTransactionManager(driver: Driver): Neo4jTransactionManager {
        return Neo4jTransactionManager(driver)
    }
}
