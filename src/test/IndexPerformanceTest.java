package test;

import lib.*;
import org.junit.jupiter.api.Test;
import java.util.*;

public class IndexPerformanceTest {

    @Test
    void testPerformanceComparison() {
        UserQuery query = new UserQuery();

        // Buscar por ID 1000 vezes
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            List<Users> result = query.from(
                    query.equals("id", (i % 30) + 1)
            );
        }
        long end = System.nanoTime();

        double timeMs = (end - start) / 1_000_000.0;
        System.out.println("1000 buscas: " + timeMs + " ms");
        System.out.println("Média por busca: " + (timeMs / 1000) + " ms");

        // Mostrar estatísticas dos índices
        System.out.println("\n" + query.getIndexStats());
    }
}
