import java.sql.Connection;
import java.sql.SQLException;

public interface IAdapter {//abstraccion mas fuerte del adapter puede no servir
    Connection getConnection(String host, int port, String database, String user, String password) throws SQLException;

    default Connection getConnection(DbConfig config) throws SQLException {
        if (config == null) throw new IllegalArgumentException("DbConfig no puede ser null");
        return getConnection(config.getHost(), config.getPort(), config.getDatabase(), config.getUser(), config.getPassword());
    }
}

//soporte ymal, toml, json.