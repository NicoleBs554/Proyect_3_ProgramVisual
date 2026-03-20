import java.util.List;
import java.util.Map;

public class Demo {
    public static void main(String[] args) {
        try {
            // ========== PostgreSQL ==========
            System.out.println("=== PostgreSQL ===");
            DbComponent<PostgresAdapter> pg = new DbComponent<>(
                "PostgresAdapter",
                "localhost", 5432, "testdb",
                "postgres", "password",
                "queries_postgres.properties", 5
            );

            pg.query("createTable");
            pg.query("insertUser", "Juan", 30);
            pg.query("insertUser", "Ana", 25);

            List<Map<String, Object>> usersPg = pg.query("findAllUsers");
            usersPg.forEach(row -> System.out.println(row.get("name") + " (" + row.get("age") + ")"));

            pg.shutdown();

            // ========== H2 ==========
            System.out.println("\n=== H2 ===");
            DbComponent<H2Adapter> h2 = new DbComponent<>(
                "H2Adapter",
                "", 0, "testdb", "sa", "",
                "queries_h2.properties", 3
            );

            h2.query("createTable");
            h2.query("insertUser", "Carlos", 28);
            List<Map<String, Object>> usersH2 = h2.query("findAllUsers");
            usersH2.stream().map(row -> row.get("name") + " (" + row.get("age") + ")").forEach(System.out::println);
            h2.shutdown();

            // ========== MySQL ==========
            System.out.println("\n=== MySQL ===");
            DbComponent<MySQLAdapter> mysql = new DbComponent<>(
                "MySQLAdapter",
                "localhost", 3306, "testdb",
                "root", "root",
                "queries_mysql.properties", 4
            );

            mysql.query("createTable");
            mysql.query("insertUser", "Luis", 32);
            List<Map<String, Object>> usersMysql = mysql.query("findAllUsers");
            usersMysql.stream().map(row -> row.get("name") + " (" + row.get("age") + ")").forEach(System.out::println);
            mysql.shutdown();

            // Generar documentación
            Generator.generarMarkdownParaClase(DbComponent.class, "DbComponent.md");
            Generator.generarMarkdownParaClase(SimpleConnectionPool.class, "SimpleConnectionPool.md");
            System.out.println("\nDocumentación generada en archivos .md");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}