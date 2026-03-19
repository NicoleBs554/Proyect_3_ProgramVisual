import java.sql.Connection;
import java.sql.SQLException;

public interface IAdapter {
    Connection getConnection(String host, int port, String database, String user, String password) throws SQLException;
}