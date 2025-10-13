package com.alotra.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures new columns for admin reply on DanhGia table exist (idempotent for SQL Server).
 * Columns created when missing:
 * - TraLoiAdmin NVARCHAR(MAX) NULL
 * - TraLoiLuc   DATETIME2 NULL
 * - TraLoiBoi   NVARCHAR(255) NULL
 */
@Component
public class ReviewReplySchemaInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ReviewReplySchemaInitializer.class);
    private final JdbcTemplate jdbc;

    public ReviewReplySchemaInitializer(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void run(ApplicationArguments args) {
        try {
            // SQL Server specific: COL_LENGTH returns NULL when the column doesn't exist
            String addReply = "IF COL_LENGTH('DanhGia','TraLoiAdmin') IS NULL ALTER TABLE DanhGia ADD TraLoiAdmin NVARCHAR(MAX) NULL";
            String addAt    = "IF COL_LENGTH('DanhGia','TraLoiLuc') IS NULL ALTER TABLE DanhGia ADD TraLoiLuc DATETIME2 NULL";
            String addBy    = "IF COL_LENGTH('DanhGia','TraLoiBoi') IS NULL ALTER TABLE DanhGia ADD TraLoiBoi NVARCHAR(255) NULL";
            jdbc.execute(addReply);
            jdbc.execute(addAt);
            jdbc.execute(addBy);
        } catch (Exception e) {
            // Don't fail the app if migration cannot run; log for operator
            log.warn("Could not ensure DanhGia reply columns: {}", e.getMessage());
        }
    }
}
