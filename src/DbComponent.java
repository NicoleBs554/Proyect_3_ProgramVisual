import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.*;

@InfoClase(
    nombre = "DbComponent",
    autor = "Tu Nombre",
    descripcion = "Componente genérico de base de datos con pool interno y consultas predefinidas.",
    version = "1.0",
    esSubclase = false
)
public class DbComponent<T extends IAdapter> {
    @InfoAtributo(tipo = "T", descripcion = "Adaptador concreto para la base de datos", modificadores = {"private", "final"})
    private final T adapter;
    @InfoAtributo(tipo = "ConnectionPool", descripcion = "Pool de conexiones interno", modificadores = {"private", "final"})
    private final ConnectionPool pool;
    @InfoAtributo(tipo = "Map<String, String>", descripcion = "Mapa de consultas predefinidas", modificadores = {"private", "final"})
    private final Map<String, String> queries = new HashMap<>();

    @SuppressWarnings("unchecked") // Se suprime la advertencia porque ya verificamos que la clase implementa IAdapter
    @InfoMetodo(
        parametros = {"String adapterClassName", "String host", "int port", "String database", "String user", "String password", "String queriesFilePath", "int poolSize"},
        tipoRetorno = "",
        descripcion = "Constructor que recibe todos los datos de conexión y la ruta del archivo de queries. Usa reflexión para instanciar el adaptador.",
        modificadores = {"public"},
        esConstructor = true
    )
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
        try (InputStream input = new FileInputStream(path)) {
            Properties props = new Properties();
            props.load(input);
            for (String key : props.stringPropertyNames()) {
                queries.put(key, props.getProperty(key));
            }
        }
    }

    @InfoMetodo( //columnas de mas crea resulset donde no es
        parametros = {"String name", "Object... params"},
        tipoRetorno = "List<Map<String, Object>>",
        descripcion = "Ejecuta una consulta predefinida por nombre (sin transacción). Obtiene una conexión del pool y la libera al finalizar.",
        modificadores = {"public"},
        esGetter = false,
        esSetter = false
    )
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

    @InfoMetodo(
        parametros = {},
        tipoRetorno = "Transaction",
        descripcion = "Inicia una transacción. Reserva una conexión del pool que se mantendrá hasta commit/rollback/close.",
        modificadores = {"public"},
        esGetter = false,
        esSetter = false
    )
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

    @InfoMetodo(
        parametros = {},
        tipoRetorno = "void",
        descripcion = "Cierra el pool y libera todos los recursos.",
        modificadores = {"public"},
        esGetter = false,
        esSetter = false
    )
    public void shutdown() {
        pool.shutdown();
    }

    @InfoClase(
        nombre = "Transaction",
        autor = "Tu Nombre",
        descripcion = "Clase interna que representa una transacción. Permite ejecutar múltiples consultas dentro de una misma conexión y confirmar o revertir los cambios.",
        version = "1.0",
        esSubclase = true
    )
    public class Transaction implements AutoCloseable {
        @InfoAtributo(tipo = "Connection", descripcion = "Conexión reservada para la transacción", modificadores = {"private", "final"})
        private final Connection conn;
        @InfoAtributo(tipo = "ConnectionPool", descripcion = "Pool al que pertenece la conexión", modificadores = {"private", "final"})
        private final ConnectionPool pool;
        @InfoAtributo(tipo = "Map<String, String>", descripcion = "Mapa de consultas", modificadores = {"private", "final"})
        private final Map<String, String> queries;
        @InfoAtributo(tipo = "boolean", descripcion = "Indica si la transacción ya fue cerrada", modificadores = {"private"})
        private boolean closed = false;

        private Transaction(Connection conn, ConnectionPool pool, Map<String, String> queries) {
            this.conn = conn;
            this.pool = pool;
            this.queries = queries;
        }

        @InfoMetodo(
            parametros = {"String name", "Object... params"},
            tipoRetorno = "List<Map<String, Object>>",
            descripcion = "Ejecuta una consulta dentro de la transacción.",
            modificadores = {"public"},
            esGetter = false,
            esSetter = false
        )
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

        @InfoMetodo(
            parametros = {},
            tipoRetorno = "void",
            descripcion = "Confirma los cambios de la transacción y libera la conexión.",
            modificadores = {"public"},
            esGetter = false,
            esSetter = false
        )
        public void commit() throws SQLException {
            if (closed) throw new SQLException("Transacción ya cerrada");
            try {
                conn.commit();
            } finally {
                close();
            }
        }

        @InfoMetodo(
            parametros = {},
            tipoRetorno = "void",
            descripcion = "Revertir los cambios de la transacción y libera la conexión.",
            modificadores = {"public"},
            esGetter = false,
            esSetter = false
        )
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
                        conn.rollback(); // rollback implícito si no se hizo commit
                    }
                } catch (SQLException ignored) {}
                pool.releaseConnection(conn);
            }
        }
    }
}