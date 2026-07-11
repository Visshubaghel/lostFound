package tests;

import org.junit.jupiter.api.Test;
import models.Item;
import static org.junit.jupiter.api.Assertions.*;

public class ItemTest {
    @Test
    public void testItemEncapsulation() {
        Item item = new Item("item123", "QR123", "A lost wallet", new double[]{0.5, 0.5}, "wallet.jpg", true);
        
        assertEquals("item123", item.getId(), "ID encapsulation failed");
        assertEquals("QR123", item.getQrToken(), "QR Token encapsulation failed");
        assertEquals("A lost wallet", item.getTextDescription(), "Description encapsulation failed");
        assertArrayEquals(new double[]{0.5, 0.5}, item.getEmbeddingVector(), "Vector array encapsulation failed");
        assertEquals("wallet.jpg", item.getPhotoPath(), "Photo path encapsulation failed");
        assertTrue(item.isFoundItem(), "Boolean flag encapsulation failed");
    }
}
