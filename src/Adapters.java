import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// Adaptador para PostgreSQL
class PostgresAdapter implements IAdapter {
    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL JDBC Driver not found", e);
        }
    }

    @Override
    public Connection getConnection(String host, int port, String database, String user, String password) throws SQLException {
        String url = "jdbc:postgresql://" + host + ":" + port + "/" + database;
        return DriverManager.getConnection(url, user, password);
    }
}

// Adaptador para H2
class H2Adapter implements IAdapter {
    static {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("H2 JDBC Driver not found", e);
        }
    }

    @Override
    public Connection getConnection(String host, int port, String database, String user, String password) throws SQLException {
        // Para H2 se usa una ruta de archivo; host y port se ignoran.
        String url = "jdbc:h2:./" + database;
        return DriverManager.getConnection(url, user, password);
    }
}