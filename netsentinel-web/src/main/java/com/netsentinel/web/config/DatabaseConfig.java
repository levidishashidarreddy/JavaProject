package com.netsentinel.web.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${spring.datasource.url:jdbc:sqlite:database/netsentinel.db}")
    private String dbUrl;

    @Bean
    public DataSource dataSource(ResourceLoader resourceLoader) {
        try {
            // Ensure directory exists
            String dbPath = dbUrl.replace("jdbc:sqlite:", "");
            File dbFile = new File(dbPath);
            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
                log.info("Created database directory: {}", parentDir.getAbsolutePath());
            }

            boolean isNewDb = !dbFile.exists() || dbFile.length() == 0;

            SQLiteDataSource ds = new SQLiteDataSource();
            ds.setUrl(dbUrl);
            ds.setEnforceForeignKeys(true);

            // Test connection and set pragma
            try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
                stmt.execute("PRAGMA journal_mode = WAL;");
            }

            if (isNewDb) {
                log.info("Initializing brand new NetSentinel database at {}", dbFile.getAbsolutePath());
                Resource schemaRes = resourceLoader.getResource("classpath:schema.sql");
                Resource dataRes = resourceLoader.getResource("classpath:data.sql");

                ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                if (schemaRes.exists()) {
                    populator.addScript(schemaRes);
                }
                if (dataRes.exists()) {
                    populator.addScript(dataRes);
                }
                populator.setContinueOnError(true);
                populator.execute(ds);
                log.info("Successfully executed database schema and demo seed scripts.");
            } else {
                log.info("Existing NetSentinel database detected at {}", dbFile.getAbsolutePath());
                // Still ensure tables exist
                Resource schemaRes = resourceLoader.getResource("classpath:schema.sql");
                if (schemaRes.exists()) {
                    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                    populator.addScript(schemaRes);
                    populator.setContinueOnError(true);
                    populator.execute(ds);
                }
            }

            return ds;
        } catch (Exception e) {
            log.error("Fatal error initializing SQLite DataSource", e);
            throw new RuntimeException("Could not initialize SQLite database", e);
        }
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}

