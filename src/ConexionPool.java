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

// Implementación con tamaño fijo (mínimo = máximo)
class SimpleConnectionPool implements ConnectionPool {
    private final IAdapter adapter;
    private final String host, database, user, password;
    private final int port, maxSize;
    private final BlockingQueue<Connection> idle = new LinkedBlockingQueue<>();
    private final AtomicInteger total = new AtomicInteger(0);
    private volatile boolean closed = false;

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

    private Connection createConnection() throws SQLException {
        return adapter.getConnection(host, port, database, user, password);
    }

    @Override
    public Connection getConnection() throws InterruptedException, SQLException {
        if (closed) throw new IllegalStateException("Pool cerrado");
        Connection conn = idle.poll();
        if (conn != null) return conn;

        while (true) {
            int current = total.get();
            if (current >= maxSize) {
                return idle.take();
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

    @Override
    public void releaseConnection(Connection connection) {
        if (connection == null || closed) {
            try { if (connection != null) connection.close(); } catch (SQLException ignored) {}
            return;
        }
        try {
            if (connection.isClosed() || !connection.isValid(2)) {
                total.decrementAndGet();
                connection.close();
                return;
            }
        } catch (SQLException e) {
            total.decrementAndGet();
            try { connection.close(); } catch (SQLException ignored) {}
            return;
        }
        idle.offer(connection);
    }

    @Override
    public void shutdown() {
        closed = true;
        Connection conn;
        while ((conn = idle.poll()) != null) {
            try { conn.close(); } catch (SQLException ignored) {}
        }
    }
}