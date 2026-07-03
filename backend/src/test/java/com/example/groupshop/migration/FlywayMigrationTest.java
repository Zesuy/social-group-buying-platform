package com.example.groupshop.migration;

import com.example.groupshop.base.ServiceTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Flyway migration and table structure.
 * Verifies that all 12 MVP core tables are created with proper
 * primary keys, unique constraints, and indexes.
 */
class FlywayMigrationTest extends ServiceTestBase {

    @Autowired
    private DataSource dataSource;

    /**
     * All 12 MVP core table names.
     */
    private static final Set<String> EXPECTED_TABLES = Set.of(
            "users", "leaders", "stores", "products",
            "group_buys", "group_buy_items", "addresses",
            "orders", "order_items", "shipments",
            "subscriptions", "member_relations"
    );

    /**
     * Tables with their expected unique constraints (columns).
     */
    private static final Map<String, List<String>> EXPECTED_UNIQUE_CONSTRAINTS = Map.of(
            "leaders", List.of("user_id"),
            "stores", List.of("leader_id"),
            "orders", List.of("order_no"),
            "subscriptions", List.of("user_id", "leader_id"),
            "member_relations", List.of("user_id", "store_id")
    );

    @Test
    void allExpectedTablesExist() throws Exception {
        Set<String> actualTables = getTableNames();
        for (String table : EXPECTED_TABLES) {
            assertThat(actualTables)
                    .as("Table '%s' should exist after Flyway migration", table)
                    .contains(table);
        }
    }

    @Test
    void usersTable_hasRequiredColumns() throws Exception {
        Set<String> columns = getColumnNames("users");
        assertThat(columns)
                .contains("id", "nickname", "avatar_url", "phone", "status", "created_at", "updated_at");
    }

    @Test
    void leadersTable_hasRequiredColumns() throws Exception {
        Set<String> columns = getColumnNames("leaders");
        assertThat(columns)
                .contains("id", "user_id", "display_name", "avatar_url", "bio",
                        "service_status", "member_count", "follower_count", "created_at", "updated_at");
    }

    @Test
    void storesTable_hasRequiredColumns() throws Exception {
        Set<String> columns = getColumnNames("stores");
        assertThat(columns)
                .contains("id", "leader_id", "name", "logo_url", "description",
                        "default_delivery_type", "distribution_enabled", "status", "created_at", "updated_at");
    }

    @Test
    void productsTable_hasRequiredColumns() throws Exception {
        Set<String> columns = getColumnNames("products");
        assertThat(columns)
                .contains("id", "store_id", "name", "description", "cover_image_url",
                        "detail_image_urls",
                        "base_price_amount", "stock", "status", "created_at", "updated_at");
    }

    @Test
    void groupBuysTable_hasRequiredColumns() throws Exception {
        Set<String> columns = getColumnNames("group_buys");
        assertThat(columns)
                .contains("id", "store_id", "leader_id", "title", "introduction",
                        "cover_image_url", "gallery_image_urls", "content_blocks",
                        "group_type", "delivery_type", "shipping_time",
                        "start_time", "end_time", "visibility", "status", "created_at", "updated_at");
    }

    @Test
    void groupBuyItemsTable_hasRequiredColumns() throws Exception {
        Set<String> columns = getColumnNames("group_buy_items");
        assertThat(columns)
                .contains("id", "group_buy_id", "product_id", "display_name",
                        "group_price_amount", "group_stock", "sold_count", "sort_order",
                        "created_at", "updated_at");
    }

    @Test
    void addressesTable_hasRequiredColumns() throws Exception {
        Set<String> columns = getColumnNames("addresses");
        assertThat(columns)
                .contains("id", "user_id", "receiver_name", "receiver_phone",
                        "province", "city", "district", "detail", "is_default", "created_at", "updated_at");
    }

    @Test
    void ordersTable_hasRequiredColumns() throws Exception {
        Set<String> columns = getColumnNames("orders");
        assertThat(columns)
                .contains("id", "order_no", "user_id", "leader_id", "store_id", "group_buy_id",
                        "address_id", "receiver_name", "receiver_phone",
                        "province", "city", "district", "detail", "full_address",
                        "total_amount", "discount_amount", "pay_amount",
                        "pay_status", "order_status", "remark",
                        "paid_at", "shipped_at", "completed_at", "created_at", "updated_at");
    }

    @Test
    void orderItemsTable_hasRequiredColumns() throws Exception {
        Set<String> columns = getColumnNames("order_items");
        assertThat(columns)
                .contains("id", "order_id", "product_id", "group_buy_item_id",
                        "product_name", "sku_name", "unit_price_amount", "quantity", "total_amount",
                        "created_at");
    }

    @Test
    void shipmentsTable_hasRequiredColumns() throws Exception {
        Set<String> columns = getColumnNames("shipments");
        assertThat(columns)
                .contains("id", "order_id", "delivery_type", "logistics_company",
                        "tracking_no", "shipped_by", "shipped_at", "created_at");
    }

    @Test
    void subscriptionsTable_hasRequiredColumns() throws Exception {
        Set<String> columns = getColumnNames("subscriptions");
        assertThat(columns)
                .contains("id", "user_id", "leader_id", "store_id",
                        "status", "source", "subscribed_at", "canceled_at", "created_at", "updated_at");
    }

    @Test
    void memberRelationsTable_hasRequiredColumns() throws Exception {
        Set<String> columns = getColumnNames("member_relations");
        assertThat(columns)
                .contains("id", "user_id", "leader_id", "store_id",
                        "level_name", "growth_value", "total_order_amount", "total_orders",
                        "last_order_at", "created_at", "updated_at");
    }

    @Test
    void leadersHasUniqueConstraintOnUserId() throws Exception {
        assertUniqueConstraintExists("leaders", "user_id");
    }

    @Test
    void storesHasUniqueConstraintOnLeaderId() throws Exception {
        assertUniqueConstraintExists("stores", "leader_id");
    }

    @Test
    void ordersHasUniqueConstraintOnOrderNo() throws Exception {
        assertUniqueConstraintExists("orders", "order_no");
    }

    @Test
    void subscriptionsHasUniqueConstraintOnUserAndLeader() throws Exception {
        assertUniqueConstraintExists("subscriptions", "user_id", "leader_id");
    }

    @Test
    void memberRelationsHasUniqueConstraintOnUserAndStore() throws Exception {
        assertUniqueConstraintExists("member_relations", "user_id", "store_id");
    }

    // ── Helper methods ─────────────────────────────────────────────

    private Set<String> getTableNames() throws Exception {
        Set<String> tables = new HashSet<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getTables(null, null, "%", new String[]{"TABLE"});
            while (rs.next()) {
                String name = rs.getString("TABLE_NAME").toLowerCase();
                // H2 may return uppercase; normalize to lowercase
                tables.add(name);
            }
        }
        return tables;
    }

    private Set<String> getColumnNames(String tableName) throws Exception {
        Set<String> columns = new LinkedHashSet<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getColumns(null, null, tableName.toUpperCase(), "%");
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME").toLowerCase());
            }
            // Fallback: try lowercase table name if uppercase returned nothing
            if (columns.isEmpty()) {
                rs = meta.getColumns(null, null, tableName.toLowerCase(), "%");
                while (rs.next()) {
                    columns.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
            }
        }
        return columns;
    }

    private void assertUniqueConstraintExists(String tableName, String... columns) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getIndexInfo(null, null, tableName.toUpperCase(), true, false);

            // If nothing found, try lowercase
            if (!rs.next()) {
                rs = meta.getIndexInfo(null, null, tableName.toLowerCase(), true, false);
                rs.next();
            }

            // Collect non-null column names from this unique index
            List<String> indexColumns = new ArrayList<>();
            do {
                String colName = rs.getString("COLUMN_NAME");
                if (colName != null) {
                    indexColumns.add(colName.toLowerCase());
                }
            } while (rs.next());

            assertThat(indexColumns)
                    .as("Unique constraint on %s should include columns %s", tableName, Arrays.toString(columns))
                    .containsAll(Arrays.asList(columns));
        }
    }
}
