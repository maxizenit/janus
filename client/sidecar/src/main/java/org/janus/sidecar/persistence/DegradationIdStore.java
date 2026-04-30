package org.janus.sidecar.persistence;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;

@RequiredArgsConstructor
@Slf4j
@NullMarked
public class DegradationIdStore {

  private final DataSource dataSource;

  public void init() {
    try (Connection conn = dataSource.getConnection();
        var stmt = conn.createStatement()) {
      stmt.execute(
          "CREATE TABLE IF NOT EXISTS degradation_ids (degradation_id TEXT PRIMARY KEY NOT NULL)");
      log.info("DegradationIdStore initialized");
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to initialize degradation ID store", e);
    }
  }

  public void replaceAll(Set<String> ids) {
    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      try {
        try (var deleteStmt = conn.createStatement()) {
          deleteStmt.execute("DELETE FROM degradation_ids");
        }

        if (!ids.isEmpty()) {
          try (var insertStmt =
              conn.prepareStatement("INSERT INTO degradation_ids (degradation_id) VALUES (?)")) {
            for (String id : ids) {
              insertStmt.setString(1, id);
              insertStmt.addBatch();
            }
            insertStmt.executeBatch();
          }
        }

        conn.commit();
        log.debug("Persisted {} degradation IDs", ids.size());
      } catch (SQLException e) {
        conn.rollback();
        throw e;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to persist degradation IDs", e);
    }
  }

  public Set<String> loadAll() {
    var ids = new HashSet<String>();
    try (Connection conn = dataSource.getConnection();
        var stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT degradation_id FROM degradation_ids")) {
      while (rs.next()) {
        ids.add(rs.getString(1));
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to load degradation IDs", e);
    }
    log.debug("Loaded {} degradation IDs from store", ids.size());
    return ids;
  }
}
