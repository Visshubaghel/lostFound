package tests;

import org.junit.jupiter.api.Test;
import utils.CosineSimilarity;
import static org.junit.jupiter.api.Assertions.*;

public class CosineSimilarityTest {
    @Test
    public void testIdenticalVectors() {
        double[] v1 = {1.0, 2.0, 3.0};
        double[] v2 = {1.0, 2.0, 3.0};
        assertEquals(1.0, CosineSimilarity.calculate(v1, v2), 0.0001, "Identical vectors should have similarity of 1.0");
    }

    @Test
    public void testOrthogonalVectors() {
        double[] v1 = {1.0, 0.0};
        double[] v2 = {0.0, 1.0};
        assertEquals(0.0, CosineSimilarity.calculate(v1, v2), 0.0001, "Orthogonal vectors should have similarity of 0.0");
    }

    @Test
    public void testOppositeVectors() {
        double[] v1 = {1.0, 2.0};
        double[] v2 = {-1.0, -2.0};
        assertEquals(-1.0, CosineSimilarity.calculate(v1, v2), 0.0001, "Opposite vectors should have similarity of -1.0");
    }
}
