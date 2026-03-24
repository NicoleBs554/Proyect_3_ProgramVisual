import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
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

    @SuppressWarnings("unchecked")
    @InfoMetodo(
        parametros = {"String adapterClassName", "String host", "int port", "String database", "String user", "String password", "String queriesFilePath", "int poolSize"},
        tipoRetorno = "",
        descripcion = "Constructor que recibe todos los datos de conexión y la ruta del archivo de queries. Usa reflexión para instanciar el adaptador.",
        modificadores = {"public"},
        esConstructor = true
    )
    public DbComponent(String adapterClassName, String host, int port, String database,
                       String user, String password, String queriesFilePath, int poolSize) throws Exception {
        // Cargar la clase y verificar que implementa IAdapter
        Class<?> clazz = Class.forName(adapterClassName);
        if (!IAdapter.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("La clase " + adapterClassName + " no implementa IAdapter");
        }
        this.adapter = (T) clazz.getDeclaredConstructor().newInstance();
        this.pool = new SimpleConnectionPool(adapter, host, port, database, user, password, poolSize);
        loadQueries(queriesFilePath);
    }

    /**
     * Carga las consultas desde un archivo según su extensión.
     * Soporta .properties, .json, .yaml/.yml y .toml.
     */
    private void loadQueries(String path) throws IOException {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Ruta de queries vacía");
        }
        int dot = path.lastIndexOf('.');
        if (dot < 0 || dot == path.length() - 1) {
            throw new IllegalArgumentException("Extensión inválida en archivo de queries: " + path);
        }

        String ext = path.substring(dot + 1).toLowerCase(Locale.ROOT);
        switch (ext) {
            case "properties":
                loadProperties(path);
                break;
            case "json":
                loadJson(path);
                break;
            case "yaml":
            case "yml":
                loadYaml(path);
                break;
            case "toml":
                loadToml(path);
                break;
            default:
                throw new IllegalArgumentException("Formato no soportado: " + ext);
        }
    }

    private List<Map<String, Object>> readRows(ResultSet rs) throws SQLException {
        List<Map<String, Object>> out = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= cols; i++) {
                String label = meta.getColumnLabel(i);
                Object value = rs.getObject(i);
                row.put(label, value);
                if (label != null) {
                    row.put(label.toLowerCase(Locale.ROOT), value);
                }
            }
            out.add(row);
        }
        return out;
    }

    // Carga archivo .properties
    private void loadProperties(String path) throws IOException {
        try (InputStream input = new FileInputStream(path)) {
            Properties props = new Properties();
            props.load(input);
            for (String key : props.stringPropertyNames()) {
                queries.put(key, props.getProperty(key));
            }
        }
    }

    // Carga JSON sin import estático
    @SuppressWarnings("unchecked")
    private void loadJson(String path) throws IOException {
        try {
            Class<?> mapperClass = Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
            Object mapper = mapperClass.getDeclaredConstructor().newInstance();
            Method readValue = mapperClass.getMethod("readValue", File.class, Class.class);
            Map<String, Object> map = (Map<String, Object>) readValue.invoke(mapper, new File(path), Map.class);
            map.forEach((k, v) -> queries.put(String.valueOf(k), String.valueOf(v)));
        } catch (ClassNotFoundException e) {
            throw new IOException("Falta Jackson en classpath para leer JSON", e);
        } catch (Exception e) {
            throw new IOException("Error leyendo JSON: " + path, e);
        }
    }

    // Carga YAML sin import estático
    @SuppressWarnings("unchecked")
    private void loadYaml(String path) throws IOException {
        try (InputStream input = new FileInputStream(path)) {
            Class<?> yamlClass = Class.forName("org.yaml.snakeyaml.Yaml");
            Object yaml = yamlClass.getDeclaredConstructor().newInstance();
            Method load = yamlClass.getMethod("load", InputStream.class);
            Object obj = load.invoke(yaml, input);
            if (!(obj instanceof Map)) {
                throw new IOException("YAML debe contener un mapa clave->valor");
            }
            Map<String, Object> map = (Map<String, Object>) obj;
            map.forEach((k, v) -> queries.put(String.valueOf(k), String.valueOf(v)));
        } catch (ClassNotFoundException e) {
            throw new IOException("Falta SnakeYAML en classpath para leer YAML", e);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Error leyendo YAML: " + path, e);
        }
    }

    // Carga TOML sin import estático
    @SuppressWarnings("unchecked")
    private void loadToml(String path) throws IOException {
        try {
            Class<?> tomlClass = Class.forName("com.moandjiezana.toml.Toml");
            Object toml = tomlClass.getDeclaredConstructor().newInstance();
            Method read = tomlClass.getMethod("read", File.class);
            Object parsed = read.invoke(toml, new File(path));
            Method toMap = tomlClass.getMethod("toMap");
            Map<String, Object> map = (Map<String, Object>) toMap.invoke(parsed);
            map.forEach((k, v) -> queries.put(String.valueOf(k), String.valueOf(v)));
        } catch (ClassNotFoundException e) {
            throw new IOException("Falta toml4j en classpath para leer TOML", e);
        } catch (Exception e) {
            throw new IOException("Error leyendo TOML: " + path, e);
        }
    }

    @InfoMetodo(
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
        try {
            conn = pool.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                boolean hasResult = stmt.execute();
                if (hasResult) {
                    try (ResultSet rs = stmt.getResultSet()) {
                        return readRows(rs);
                    }
                }
                return Collections.emptyList();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupción esperando conexión", e);
        } finally {
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
            throw new SQLException("Interrupción esperando conexión", e);
        }
        try {
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            pool.releaseConnection(conn);
            throw e;
        }
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

        private boolean completed = false;

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
                boolean hasResult = stmt.execute();
                if (hasResult) {
                    try (ResultSet rs = stmt.getResultSet()) {
                        return readRows(rs);
                    }
                }
                return Collections.emptyList();
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
                completed = true;
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
                completed = true;
            } finally {
                close();
            }
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            try {
                if (!completed) {
                    try { conn.rollback(); } catch (SQLException ignored) {}
                }
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
            } finally {
                pool.releaseConnection(conn);
            }
        }
    }
}