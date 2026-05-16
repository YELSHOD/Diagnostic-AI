package com.yelshod.diagnosticserviceai.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

public class PostgresDatabaseAutoCreateEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String POSTGRES_JDBC_PREFIX = "jdbc:postgresql://";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.getProperty("app.database.auto-create", Boolean.class, false)) {
            return;
        }

        String url = environment.getProperty("spring.datasource.url");
        if (url == null || !url.startsWith(POSTGRES_JDBC_PREFIX)) {
            return;
        }

        String username = environment.getProperty("spring.datasource.username");
        String password = environment.getProperty("spring.datasource.password");
        DatabaseUrl databaseUrl = parseDatabaseUrl(url);

        if (databaseUrl.databaseName().isBlank()) {
            return;
        }

        if (canConnect(url, username, password)) {
            return;
        }

        String maintenanceDatabase = environment.getProperty(
                "app.database.maintenance-database",
                "postgres"
        );
        String maintenanceUrl = databaseUrl.withDatabase(maintenanceDatabase);
        createDatabaseIfMissing(maintenanceUrl, username, password, databaseUrl.databaseName());
    }

    private static boolean canConnect(String url, String username, String password) {
        try (Connection ignored = DriverManager.getConnection(url, username, password)) {
            return true;
        } catch (SQLException ignored) {
            return false;
        }
    }

    private static void createDatabaseIfMissing(
            String maintenanceUrl,
            String username,
            String password,
            String databaseName
    ) {
        try (Connection connection = DriverManager.getConnection(maintenanceUrl, username, password)) {
            if (databaseExists(connection, databaseName)) {
                return;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE DATABASE " + quoteIdentifier(databaseName));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Failed to auto-create PostgreSQL database '%s'. Check DB_USER/DB_PASSWORD or create it manually."
                            .formatted(databaseName),
                    exception
            );
        }
    }

    private static boolean databaseExists(Connection connection, String databaseName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM pg_database WHERE datname = ?"
        )) {
            statement.setString(1, databaseName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private static DatabaseUrl parseDatabaseUrl(String url) {
        int databaseStart = url.indexOf('/', POSTGRES_JDBC_PREFIX.length());
        if (databaseStart < 0) {
            return new DatabaseUrl(url, "", "");
        }

        int queryStart = url.indexOf('?', databaseStart);
        String databaseName = queryStart < 0
                ? url.substring(databaseStart + 1)
                : url.substring(databaseStart + 1, queryStart);
        String query = queryStart < 0 ? "" : url.substring(queryStart);

        return new DatabaseUrl(url.substring(0, databaseStart + 1), databaseName, query);
    }

    private record DatabaseUrl(String prefix, String databaseName, String query) {

        String withDatabase(String database) {
            return prefix + database + query;
        }
    }
}
