package tests;

import org.junit.jupiter.api.Test;
import models.Item;
import models.Match;
import static org.junit.jupiter.api.Assertions.*;

public class MatchTest {
    @Test
    public void testMatchCreationAndRetrieval() {
        Item item = new Item("id1", "token1", "desc", new double[]{1.0}, "photo.jpg", false);
        Match<Item> match = new Match<>(item, 0.95);
        
        assertEquals(item, match.getItem(), "Item should be perfectly encapsulated inside Match via Generics");
        assertEquals(0.95, match.getSimilarityScore(), "Similarity score should match initialization value");
    }
}
