import java.util.concurrent.*;
import java.util.List;
import java.util.Map;

public class LoadTest {
    public static void main(String[] args) throws Exception {
        // Ajusta las credenciales según tu entorno
        DbComponent<PostgresAdapter> db = new DbComponent<>(
            "PostgresAdapter",
            "localhost", 5432, "testdb",
            "postgres", "Peroqueconio12",    // ← Cambia por la contraseña correcta
            "queries_postgres.properties", 10
        );

        int numThreads = 100;   // Número de consultas concurrentes
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Todos los hilos esperan la señal
                    List<Map<String, Object>> results = db.query("findAllUsers");
                    // Opcional: puedes imprimir algo para verificar
                    // System.out.println("Consulta ejecutada, filas: " + results.size());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Lanza todos los hilos a la vez
        doneLatch.await();      // Espera a que terminen todos

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("Tiempo total para " + numThreads + " consultas concurrentes: " + elapsed + " ms");

        executor.shutdown();
        db.shutdown();
    }
}