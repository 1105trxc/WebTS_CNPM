package com.alotra.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures new columns exist on SuKienKhuyenMai table:
 * - DeletedAt DATETIME2 NULL (for soft delete)
 * - UrlAnh NVARCHAR(1000) NULL (banner image)
 * - LuotXem INT NULL (views)
 *
 * SQL Server compatible and idempotent.
 */
@Component
public class PromotionSchemaInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(PromotionSchemaInitializer.class);
    private final JdbcTemplate jdbc;

    public PromotionSchemaInitializer(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbc.execute("IF COL_LENGTH('SuKienKhuyenMai','DeletedAt') IS NULL ALTER TABLE SuKienKhuyenMai ADD DeletedAt DATETIME2 NULL");
            jdbc.execute("IF COL_LENGTH('SuKienKhuyenMai','UrlAnh') IS NULL ALTER TABLE SuKienKhuyenMai ADD UrlAnh NVARCHAR(1000) NULL");
            jdbc.execute("IF COL_LENGTH('SuKienKhuyenMai','LuotXem') IS NULL ALTER TABLE SuKienKhuyenMai ADD LuotXem INT NULL");
        } catch (Exception e) {
            log.warn("Could not ensure SuKienKhuyenMai columns: {}", e.getMessage());
        }
    }
}
