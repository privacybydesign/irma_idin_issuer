package org.irmacard.idin.web;

import com.google.gson.reflect.TypeToken;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class GsonJerseyProviderTest {

    public static final Annotation[] NO_ANN = new Annotation[0];
    public static final String NAME = "Ürgen";
    public static final String NAME_A = "a";
    public static final String NAME_B = "b";
    public static final String NAME_X = "x";
    public static final String NAME_Y = "y";
    public static final int COUNT_42 = 42;
    public static final int COUNT_7 = 7;
    public static final int COUNT_1 = 1;
    public static final int COUNT_2 = 2;
    public static final int COUNT_10 = 10;
    public static final int COUNT_11 = 11;

    public final GsonJerseyProvider provider = new GsonJerseyProvider();

    public static final class TestDto {
        public String name;
        public int count;

        public TestDto(final String name, final int count) {
            this.name = name;
            this.count = count;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (!(o instanceof final TestDto other)) return false;
            return count == other.count &&
                    (Objects.equals(name, other.name));
        }

        @Override
        public int hashCode() {
            return name == null ? count : name.hashCode() * 31 + count;
        }
    }

    @Test
    public void isReadable_isWriteable_alwaysTrue_forJson() {
        assertTrue(provider.isReadable(Object.class, Object.class, NO_ANN, MediaType.APPLICATION_JSON_TYPE));
        assertTrue(provider.isWriteable(Object.class, Object.class, NO_ANN, MediaType.APPLICATION_JSON_TYPE));
    }

    @Test
    public void getSize_returnsMinusOne() {
        assertEquals(-1, provider.getSize(new Object(), Object.class, Object.class, NO_ANN, MediaType.APPLICATION_JSON_TYPE));
    }

    @Test
    public void readFrom_singleObject_usesUtf8AndGenericType() throws Exception {
        final String json = "{\"name\":\"" + NAME + "\",\"count\":" + COUNT_42 + "}";
        final InputStream in = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        final Object obj = provider.readFrom(
                Object.class,
                TestDto.class,
                NO_ANN,
                MediaType.APPLICATION_JSON_TYPE,
                new MultivaluedHashMap<>(),
                in
        );

        assertInstanceOf(TestDto.class, obj);
        assertEquals(new TestDto(NAME, COUNT_42), obj);
    }

    @Test
    public void readFrom_listOfObjects_respectsGenericType() throws Exception {
        final String json = "[{\"name\":\"" + NAME_A + "\",\"count\":" + COUNT_1 + "}," +
                "{\"name\":\"" + NAME_B + "\",\"count\":" + COUNT_2 + "}]";
        final InputStream in = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        final Type listType = new TypeToken<List<TestDto>>() {
        }.getType();

        final Object obj = provider.readFrom(
                Object.class,
                listType,
                NO_ANN,
                MediaType.APPLICATION_JSON_TYPE,
                new MultivaluedHashMap<>(),
                in
        );

        assertInstanceOf(List.class, obj);
        @SuppressWarnings("unchecked") final List<TestDto> list = (List<TestDto>) obj;
        assertEquals(Arrays.asList(new TestDto(NAME_A, COUNT_1), new TestDto(NAME_B, COUNT_2)), list);
    }

    @Test
    public void writeTo_singleObject_writesUtf8Json() throws Exception {
        final TestDto dto = new TestDto(NAME, COUNT_7);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        provider.writeTo(
                dto,
                TestDto.class,
                TestDto.class,
                NO_ANN,
                MediaType.APPLICATION_JSON_TYPE,
                new MultivaluedHashMap<>(),
                out
        );

        final String json = out.toString(StandardCharsets.UTF_8);
        assertEquals("{\"name\":\"" + NAME + "\",\"count\":" + COUNT_7 + "}", json);
    }

    @Test
    public void writeTo_listOfObjects_usesProvidedGenericType() throws Exception {
        final List<TestDto> list = Arrays.asList(new TestDto(NAME_X, COUNT_10), new TestDto(NAME_Y, COUNT_11));
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        final Type listType = new TypeToken<List<TestDto>>() {
        }.getType();

        provider.writeTo(
                list,
                List.class,
                listType,
                NO_ANN,
                MediaType.APPLICATION_JSON_TYPE,
                new MultivaluedHashMap<>(),
                out
        );

        final String json = out.toString(StandardCharsets.UTF_8);
        assertEquals("[{\"name\":\"" + NAME_X + "\",\"count\":" + COUNT_10 + "}," +
                "{\"name\":\"" + NAME_Y + "\",\"count\":" + COUNT_11 + "}]", json);
    }
}
