import java.io.File;
import java.util.List;
import java.util.Map;

public class Demo {
    public static void main(String[] args) {
        String queriesPath = findQueriesPath();

        // ========== PostgreSQL ==========
        System.out.println("=== PostgreSQL ===");
        DbComponent<PostgresAdapter> pg = null;
        try {
            pg = new DbComponent<>(
                "PostgresAdapter",
                "localhost", 5432, "testdb",
                "postgres", "Peroqueconio12",
                queriesPath, 5
            );

            pg.query("createTable");
            pg.query("insertUser", "Juan", 30);
            pg.query("insertUser", "Ana", 25);

            List<Map<String, Object>> usersPg = pg.query("findAllUsers");
            usersPg.forEach(row -> System.out.println(row.get("name") + " (" + row.get("age") + ")"));
        } catch (Exception e) {
            System.err.println("PostgreSQL falló: " + e.getMessage());
        } finally {
            if (pg != null) pg.shutdown();
        }

        // ========== H2 ==========
        System.out.println("\n=== H2 ===");
        DbComponent<H2Adapter> h2 = null;
        try {
            h2 = new DbComponent<>(
                "H2Adapter",
                "", 0, "testdb", "sa", "",
                queriesPath, 3
            );

            h2.query("createTable");
            h2.query("insertUser", "Carlos", 28);
            List<Map<String, Object>> usersH2 = h2.query("findAllUsers");
            usersH2.stream().map(row -> row.get("name") + " (" + row.get("age") + ")").forEach(System.out::println);
        } catch (Exception e) {
            System.err.println("H2 falló: " + e.getMessage());
        } finally {
            if (h2 != null) h2.shutdown();
        }

        // Generar documentación
        try {
            Generator.generarMarkdownParaClase(DbComponent.class, "DbComponent.md");
            Generator.generarMarkdownParaClase(SimpleConnectionPool.class, "SimpleConnectionPool.md");
            System.out.println("\nDocumentación generada en archivos .md");
        } catch (Exception e) {
            System.err.println("No se pudo generar documentación: " + e.getMessage());
        }
    }

    private static String findQueriesPath() {
        String[] candidates = {
            "queries.properties",
            "src/queries.properties",
            "P3V/Proyect_3_ProgramVisual/queries.properties"
        };
        for (String candidate : candidates) {
            if (new File(candidate).exists()) return candidate;
        }
        return "queries.properties";
    }
}