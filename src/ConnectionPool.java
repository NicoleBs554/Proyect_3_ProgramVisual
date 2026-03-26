import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

// Interfaz del pool
interface ConnectionPool {
    Connection getConnection() throws InterruptedException, SQLException;
    void releaseConnection(Connection connection);
    void shutdown();
}

@InfoClase(
    nombre = "SimpleConnectionPool",
    autor = "Tu Nombre",
    descripcion = "Implementación de un pool de conexiones con tamaño fijo y expansión dinámica hasta un límite máximo.",
    version = "1.0",
    esSubclase = false
)
class SimpleConnectionPool implements ConnectionPool {
    @InfoAtributo(tipo = "IAdapter", descripcion = "Adaptador para crear nuevas conexiones", modificadores = {"private", "final"})
    private final IAdapter adapter;
    @InfoAtributo(tipo = "String", descripcion = "Host de la base de datos", modificadores = {"private", "final"})
    private final String host;
    @InfoAtributo(tipo = "String", descripcion = "Nombre de la base de datos", modificadores = {"private", "final"})
    private final String database;
    @InfoAtributo(tipo = "String", descripcion = "Usuario de la base de datos", modificadores = {"private", "final"})
    private final String user;
    @InfoAtributo(tipo = "String", descripcion = "Contraseña de la base de datos", modificadores = {"private", "final"})
    private final String password;
    @InfoAtributo(tipo = "int", descripcion = "Puerto de la base de datos", modificadores = {"private", "final"})
    private final int port;
    @InfoAtributo(tipo = "int", descripcion = "Tamaño máximo del pool", modificadores = {"private", "final"})
    private final int maxSize;
    @InfoAtributo(tipo = "BlockingQueue<Connection>", descripcion = "Cola de conexiones inactivas", modificadores = {"private", "final"})
    private final BlockingQueue<Connection> idle = new LinkedBlockingQueue<>();
    @InfoAtributo(tipo = "AtomicInteger", descripcion = "Número total de conexiones creadas", modificadores = {"private", "final"})
    private final AtomicInteger total = new AtomicInteger(0);
    @InfoAtributo(tipo = "boolean", descripcion = "Indica si el pool está cerrado", modificadores = {"private", "volatile"})
    private volatile boolean closed = false;

    @InfoMetodo(
        parametros = {"IAdapter adapter", "String host", "int port", "String database", "String user", "String password", "int poolSize"},
        tipoRetorno = "",
        descripcion = "Constructor que inicializa el pool con un tamaño fijo.",
        modificadores = {"public"},
        esConstructor = true
    )
    public SimpleConnectionPool(IAdapter adapter, String host, int port, String database,
                                String user, String password, int poolSize) throws SQLException {
        this.adapter = adapter;
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
        this.maxSize = poolSize;
        for (int i = 0; i < poolSize; i++) {
            idle.offer(createConnection());
            total.incrementAndGet();
        }
    }

    @InfoMetodo(
        parametros = {"IAdapter adapter", "DbConfig config", "int poolSize"},
        tipoRetorno = "",
        descripcion = "Constructor desacoplado: usa DbConfig en vez de múltiples parámetros.",
        modificadores = {"public"},
        esConstructor = true
    )
    public SimpleConnectionPool(IAdapter adapter, DbConfig config, int poolSize) throws SQLException {
        if (adapter == null) throw new IllegalArgumentException("adapter no puede ser null");
        if (config == null) throw new IllegalArgumentException("config no puede ser null");

        this.adapter = adapter;
        this.host = config.getHost();
        this.port = config.getPort();
        this.database = config.getDatabase();
        this.user = config.getUser();
        this.password = config.getPassword();
        this.maxSize = poolSize;

        for (int i = 0; i < poolSize; i++) {
            idle.offer(createConnection());
            total.incrementAndGet();
        }
    }

    private Connection createConnection() throws SQLException {
        return adapter.getConnection(host, port, database, user, password);
    }

    @InfoMetodo(
        parametros = {},
        tipoRetorno = "Connection",
        descripcion = "Obtiene una conexión del pool. Espera si no hay disponibles hasta alcanzar el límite máximo.",
        modificadores = {"public"}
    )
    @Override
    public Connection getConnection() throws InterruptedException, SQLException {
        if (closed) throw new IllegalStateException("Pool cerrado");
        while (true) {
            Connection conn = idle.poll();
            if (conn == null) break;
            if (isUsable(conn)) return conn;
            discard(conn);
        }

        while (true) {
            int current = total.get();
            if (current >= maxSize) {
                while (true) {
                    Connection conn = idle.take();
                    if (isUsable(conn)) return conn;
                    discard(conn);
                }
            }
            if (total.compareAndSet(current, current + 1)) {
                try {
                    return createConnection();
                } catch (SQLException e) {
                    total.decrementAndGet();
                    throw e;
                }
            }
        }
    }

    private boolean isUsable(Connection connection) {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    private void discard(Connection connection) {
        if (connection == null) return;
        total.decrementAndGet();
        try {
            connection.close();
        } catch (SQLException ignored) {
        }
    }

    @InfoMetodo( //crea resulset donde no es
        parametros = {"Connection connection"},
        tipoRetorno = "void",
        descripcion = "Devuelve una conexión al pool. Si la conexión está cerrada o no es válida, la descarta.",
        modificadores = {"public"}
    )
    @Override
    public void releaseConnection(Connection connection) {
        if (connection == null) return;
        if (closed) {
            try { connection.close(); } catch (SQLException ignored) {}
            return;
        }
        try {
            if (connection.isClosed() || !connection.isValid(2)) {
                total.decrementAndGet();
                connection.close();
                return;
            }
            try {
                if (!connection.getAutoCommit()) {
                    try { connection.rollback(); } catch (SQLException ignored) {}
                    connection.setAutoCommit(true);
                }
            } catch (SQLException ignored) {}
            idle.put(connection);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            try { connection.close(); } catch (SQLException ignored) {}
            total.decrementAndGet();
        } catch (SQLException e) {
            total.decrementAndGet();
            try { connection.close(); } catch (SQLException ignored) {}
        }
    }

    @InfoMetodo(
        parametros = {},
        tipoRetorno = "void",
        descripcion = "Cierra todas las conexiones y deja el pool inoperante.",
        modificadores = {"public"}
    )
    @Override
    public void shutdown() {
        closed = true;
        Connection conn;
        while ((conn = idle.poll()) != null) {
            try { conn.close(); } catch (SQLException ignored) {}
        }
    }
}