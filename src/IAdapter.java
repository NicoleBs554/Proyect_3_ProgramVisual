import java.sql.Connection;
import java.sql.SQLException;

public interface IAdapter {//abstraccion mas fuerte del adapter puede no servir
    Connection getConnection(String host, int port, String database, String user, String password) throws SQLException;
}

//soporte ymal, toml, json.