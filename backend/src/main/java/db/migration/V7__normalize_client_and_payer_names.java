package db.migration;

import dev.gustavorosa.cpsystem.utils.NameUtils;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * One-off data fix: normalizes existing names in clients.name and payments.payer_name
 * using the same rule applied to new records ({@link NameUtils#toTitleCase}).
 * Idempotent: rows already normalized are left untouched.
 */
@Slf4j
public class V7__normalize_client_and_payer_names extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws SQLException {
        Connection connection = context.getConnection();
        int clients = normalizeColumn(connection, "clients", "name");
        int payments = normalizeColumn(connection, "payments", "payer_name");
        log.info("[V7] Normalized {} clients.name and {} payments.payer_name rows", clients, payments);
    }

    private int normalizeColumn(Connection connection, String table, String column) throws SQLException {
        String select = "SELECT id, " + column + " FROM " + table;
        String update = "UPDATE " + table + " SET " + column + " = ? WHERE id = ?";
        int updated = 0;

        try (Statement selectStatement = connection.createStatement();
             ResultSet rows = selectStatement.executeQuery(select);
             PreparedStatement updateStatement = connection.prepareStatement(update)) {

            while (rows.next()) {
                long id = rows.getLong("id");
                String current = rows.getString(column);
                String normalized = NameUtils.toTitleCase(current);

                if (normalized == null || normalized.isEmpty() || normalized.equals(current)) {
                    continue;
                }

                updateStatement.setString(1, normalized);
                updateStatement.setLong(2, id);
                updateStatement.addBatch();
                updated++;
            }

            if (updated > 0) {
                updateStatement.executeBatch();
            }
        }

        return updated;
    }
}
