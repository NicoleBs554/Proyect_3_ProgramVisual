import java.util.List;
import java.util.Map;

public class Demo {
    public static void main(String[] args) {
        try {
            // --- PostgreSQL usando reflexión ---
            DbComponent<PostgresAdapter> pg = new DbComponent<>(
                "PostgresAdapter",
                "localhost", 5432, "testdb",
                "postgres", "password",
                "queries.properties", 5
            );

            // Crear tabla y datos de ejemplo
            pg.query("createTable");
            pg.query("insertUser", "Juan", 30);
            pg.query("insertUser", "Ana", 25);

            // Consulta simple (con lambda)
            List<Map<String, Object>> users = pg.query("findAllUsers");
            System.out.println("Usuarios en PostgreSQL:");
            users.forEach(row -> System.out.println(row.get("name") + " (" + row.get("age") + ")"));

            // Transacción
            try (var tx = pg.transaction()) {
                tx.query("insertUser", "Pedro", 40);
                tx.query("insertUser", "María", 35);
                tx.commit();
            }

            pg.shutdown();

            // --- H2 (otra base de datos) ---
            DbComponent<H2Adapter> h2 = new DbComponent<>(
                "H2Adapter",
                "", 0, "testdb", "sa", "",
                "queries.properties", 3
            );

            h2.query("createTable");
            h2.query("insertUser", "Carlos", 28);
            List<Map<String, Object>> h2Users = h2.query("findAllUsers");
            System.out.println("\nUsuarios en H2:");
            h2Users.stream().map(row -> row.get("name") + " (" + row.get("age") + ")").forEach(System.out::println);

            h2.shutdown();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}