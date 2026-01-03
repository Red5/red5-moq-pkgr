package org.red5.io.moq.cmaf;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;
import org.red5.io.moq.cmaf.model.FielBox;

/**
 * Tests for FielBox (Field/Frame Information Box).
 */
class FielBoxTest {

    @Test
    void testProgressiveCreation() {
        FielBox fiel = FielBox.progressive();

        assertEquals(FielBox.FIELDS_PROGRESSIVE, fiel.getFieldsPerSample());
        assertTrue(fiel.isProgressive());
        assertFalse(fiel.isInterlaced());
        assertEquals(10, fiel.getSize());
    }

    @Test
    void testInterlacedTFFCreation() {
        FielBox fiel = FielBox.interlacedTFF();

        assertEquals(FielBox.FIELDS_INTERLACED, fiel.getFieldsPerSample());
        assertEquals(FielBox.DETAIL_INTERLEAVED_TFF, fiel.getDetail());
        assertTrue(fiel.isInterlaced());
        assertTrue(fiel.isTopFieldFirst());
        assertTrue(fiel.isInterleaved());
        assertFalse(fiel.isBottomFieldFirst());
        assertFalse(fiel.isSeparated());
    }

    @Test
    void testInterlacedBFFCreation() {
        FielBox fiel = FielBox.interlacedBFF();

        assertEquals(FielBox.FIELDS_INTERLACED, fiel.getFieldsPerSample());
        assertEquals(FielBox.DETAIL_INTERLEAVED_BFF, fiel.getDetail());
        assertTrue(fiel.isInterlaced());
        assertTrue(fiel.isBottomFieldFirst());
        assertTrue(fiel.isInterleaved());
        assertFalse(fiel.isTopFieldFirst());
    }

    @Test
    void testSeparatedTFFCreation() {
        FielBox fiel = FielBox.separatedTFF();

        assertEquals(FielBox.FIELDS_INTERLACED, fiel.getFieldsPerSample());
        assertEquals(FielBox.DETAIL_SEPARATED_TFF, fiel.getDetail());
        assertTrue(fiel.isInterlaced());
        assertTrue(fiel.isTopFieldFirst());
        assertTrue(fiel.isSeparated());
        assertFalse(fiel.isInterleaved());
    }

    @Test
    void testSeparatedBFFCreation() {
        FielBox fiel = FielBox.separatedBFF();

        assertEquals(FielBox.FIELDS_INTERLACED, fiel.getFieldsPerSample());
        assertEquals(FielBox.DETAIL_SEPARATED_BFF, fiel.getDetail());
        assertTrue(fiel.isInterlaced());
        assertTrue(fiel.isBottomFieldFirst());
        assertTrue(fiel.isSeparated());
    }

    @Test
    void testSerializeProgressive() throws IOException {
        FielBox fiel = FielBox.progressive();
        byte[] data = fiel.serialize();

        assertEquals(10, data.length);

        ByteBuffer buffer = ByteBuffer.wrap(data);
        // Size
        assertEquals(10, buffer.getInt());
        // Type 'fiel'
        byte[] typeBytes = new byte[4];
        buffer.get(typeBytes);
        assertEquals("fiel", new String(typeBytes));
        // Fields
        assertEquals(1, buffer.get());
        // Detail
        assertEquals(0, buffer.get());
    }

    @Test
    void testSerializeInterlacedTFF() throws IOException {
        FielBox fiel = FielBox.interlacedTFF();
        byte[] data = fiel.serialize();

        assertEquals(10, data.length);

        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.getInt(); // size
        buffer.position(buffer.position() + 4); // skip type
        assertEquals(2, buffer.get()); // fields
        assertEquals(9, buffer.get()); // detail (interleaved TFF)
    }

    @Test
    void testSerializeInterlacedBFF() throws IOException {
        FielBox fiel = FielBox.interlacedBFF();
        byte[] data = fiel.serialize();

        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.position(8); // skip header
        assertEquals(2, buffer.get()); // fields
        assertEquals(14, buffer.get()); // detail (interleaved BFF)
    }

    @Test
    void testDeserializeProgressive() throws IOException {
        // Create raw fiel box data: size(4) + type(4) + fields(1) + detail(1)
        ByteBuffer buffer = ByteBuffer.allocate(10);
        buffer.putInt(10); // size
        buffer.put("fiel".getBytes()); // type
        buffer.put((byte) 1); // progressive
        buffer.put((byte) 0); // no detail for progressive
        buffer.flip();

        FielBox fiel = new FielBox();
        fiel.deserialize(buffer);

        assertEquals(10, fiel.getSize());
        assertEquals("fiel", fiel.getType());
        assertTrue(fiel.isProgressive());
        assertEquals(0, fiel.getDetail());
    }

    @Test
    void testDeserializeInterlaced() throws IOException {
        // Interleaved TFF (fields=2, detail=9)
        ByteBuffer buffer = ByteBuffer.allocate(10);
        buffer.putInt(10);
        buffer.put("fiel".getBytes());
        buffer.put((byte) 2); // interlaced
        buffer.put((byte) 9); // interleaved TFF
        buffer.flip();

        FielBox fiel = new FielBox();
        fiel.deserialize(buffer);

        assertTrue(fiel.isInterlaced());
        assertTrue(fiel.isTopFieldFirst());
        assertTrue(fiel.isInterleaved());
    }

    @Test
    void testRoundTrip() throws IOException {
        // Test all factory methods for round-trip serialization
        FielBox[] boxes = {
            FielBox.progressive(),
            FielBox.interlacedTFF(),
            FielBox.interlacedBFF(),
            FielBox.separatedTFF(),
            FielBox.separatedBFF()
        };

        for (FielBox original : boxes) {
            byte[] serialized = original.serialize();
            ByteBuffer buffer = ByteBuffer.wrap(serialized);

            FielBox deserialized = new FielBox();
            deserialized.deserialize(buffer);

            assertEquals(original.getFieldsPerSample(), deserialized.getFieldsPerSample(),
                "Fields mismatch for " + original);
            assertEquals(original.getDetail(), deserialized.getDetail(),
                "Detail mismatch for " + original);
        }
    }

    @Test
    void testManualConstruction() {
        FielBox fiel = new FielBox((byte) 2, (byte) 14);

        assertEquals(2, fiel.getFieldsPerSample());
        assertEquals(14, fiel.getDetail());
        assertTrue(fiel.isInterlaced());
        assertTrue(fiel.isBottomFieldFirst());
        assertTrue(fiel.isInterleaved());
    }

    @Test
    void testToString() {
        FielBox progressive = FielBox.progressive();
        assertTrue(progressive.toString().contains("progressive"));

        FielBox interlaced = FielBox.interlacedTFF();
        assertTrue(interlaced.toString().contains("interlaced"));
        assertTrue(interlaced.toString().contains("TFF"));
        assertTrue(interlaced.toString().contains("interleaved"));
    }
}
