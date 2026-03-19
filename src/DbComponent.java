import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.*;

public class DbComponent<T extends IAdapter> {
    private final T adapter;
    private final ConnectionPool pool;
    private final Map<String, String> queries = new HashMap<>();

    public DbComponent(String adapterClassName, String host, int port, String database,
                       String user, String password, String queriesFilePath, int poolSize) throws Exception {
        // Reflexión: cargar la clase y verificar que implementa IAdapter
        Class<?> clazz = Class.forName(adapterClassName);
        if (!IAdapter.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("La clase " + adapterClassName + " no implementa IAdapter");
        }
        this.adapter = (T) clazz.getDeclaredConstructor().newInstance();
        this.pool = new SimpleConnectionPool(adapter, host, port, database, user, password, poolSize);
        loadQueries(queriesFilePath);
    }

    private void loadQueries(String path) throws IOException {
        Properties props = new Properties();
        try (InputStream is = new FileInputStream(path)) {
            props.load(is);
        }
        for (String key : props.stringPropertyNames()) {
            queries.put(key, props.getProperty(key));
        }
    }

    public List<Map<String, Object>> query(String name, Object... params) throws SQLException {
        String sql = queries.get(name);
        if (sql == null) throw new IllegalArgumentException("Consulta no encontrada: " + name);

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = pool.getConnection();
            stmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            rs = stmt.executeQuery();
            List<Map<String, Object>> results = new ArrayList<>();
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                results.add(row);
            }
            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupción mientras se esperaba una conexión", e);
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (stmt != null) try { stmt.close(); } catch (SQLException ignored) {}
            if (conn != null) pool.releaseConnection(conn);
        }
    }

    public Transaction transaction() throws SQLException {
        Connection conn;
        try {
            conn = pool.getConnection();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupción mientras se esperaba una conexión", e);
        }
        conn.setAutoCommit(false);
        return new Transaction(conn, pool, queries);
    }

    public void shutdown() {
        pool.shutdown();
    }

    public class Transaction implements AutoCloseable {
        private final Connection conn;
        private final ConnectionPool pool;
        private final Map<String, String> queries;
        private boolean closed = false;

        private Transaction(Connection conn, ConnectionPool pool, Map<String, String> queries) {
            this.conn = conn;
            this.pool = pool;
            this.queries = queries;
        }

        public List<Map<String, Object>> query(String name, Object... params) throws SQLException {
            if (closed) throw new SQLException("Transacción ya cerrada");
            String sql = queries.get(name);
            if (sql == null) throw new IllegalArgumentException("Consulta no encontrada: " + name);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                if (sql.trim().toLowerCase().startsWith("select")) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<Map<String, Object>> results = new ArrayList<>();
                        ResultSetMetaData meta = rs.getMetaData();
                        int columnCount = meta.getColumnCount();
                        while (rs.next()) {
                            Map<String, Object> row = new HashMap<>();
                            for (int i = 1; i <= columnCount; i++) {
                                row.put(meta.getColumnLabel(i), rs.getObject(i));
                            }
                            results.add(row);
                        }
                        return results;
                    }
                } else {
                    stmt.executeUpdate();
                    return Collections.emptyList();
                }
            }
        }

        public void commit() throws SQLException {
            if (closed) throw new SQLException("Transacción ya cerrada");
            try {
                conn.commit();
            } finally {
                close();
            }
        }

        public void rollback() throws SQLException {
            if (closed) throw new SQLException("Transacción ya cerrada");
            try {
                conn.rollback();
            } finally {
                close();
            }
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                try {
                    if (!conn.getAutoCommit()) {
                        conn.rollback();
                    }
                } catch (SQLException ignored) {}
                pool.releaseConnection(conn);
            }
        }
    }
}