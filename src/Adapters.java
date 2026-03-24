import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

class PostgresAdapter implements IAdapter {
    static {
        try { Class.forName("org.postgresql.Driver"); }
        catch (ClassNotFoundException e) { throw new RuntimeException("PostgreSQL JDBC Driver not found", e); }
    }
    @Override
    public Connection getConnection(String host, int port, String database, String user, String password) throws SQLException {
        return DriverManager.getConnection("jdbc:postgresql://" + host + ":" + port + "/" + database, user, password);
    }
}

class H2Adapter implements IAdapter {
    static {
        try { Class.forName("org.h2.Driver"); }
        catch (ClassNotFoundException e) { throw new RuntimeException("H2 JDBC Driver not found", e); }
    }
    @Override
    public Connection getConnection(String host, int port, String database, String user, String password) throws SQLException {
        return DriverManager.getConnection("jdbc:h2:./" + database, user, password);
    }
}