package br.com.lnd.testdynamo.size;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * Calculation of json using the AWS DynamoDB strategy.
 *
 * This class calculates the total size in bytes of a json based on the syntax of a DynamoDB Item.
 */
public class ItemSizeCalculation {

    private static final String MAP = "M";
    private static final String BOOL = "BOOL";
    private static final String NUMBER = "N";
    private static final String STRING = "S";
    private static final String LIST = "L";

    public static void main(String[] args) {
        long startedInMilli = new Date().getTime();
        int totalBytes = new ItemSizeCalculation().calculateInBytes(getTestJSON());
        System.out.println("Total de bytes: " + totalBytes);
        System.out.println("In time: " + (new Date().getTime() - startedInMilli) + "ms");
    }

    /**
     * Returns the total in bytes of a json similar to DynamoDB.
     * <p>Example:
     * <em>
     * {
     *     "Item": {
     *         "active": {
     *             "BOOL": true
     *         },
     *         "publisher": {
     *             "M": {
     *                 "year": {
     *                     "N": "2019"
     *                 },
     *                 "name": {
     *                     "S": "Oreilly"
     *                 }
     *             }
     *         },
     *         "artist": {
     *             "S": "Leandro Manuel"
     *         },
     *         "age": {
     *             "N": "26"
     *         }
     *     }
     * }
     *</em></p>
     *
     * @param json The json to be calculated.
     * @return the total in bytes.
     */
    private int calculateInBytes(String json) {
        JsonObject asJsonObject = new JsonParser().parse(json).getAsJsonObject();
        JsonObject rootItem = asJsonObject.getAsJsonObject("Item");
        int totalBytes = 0;

        for (Map.Entry<String, JsonElement> item : rootItem.entrySet()) {
            totalBytes += getNestTotalByte(null, item);
        }
        return totalBytes;
    }

    private static String getTestJSON() {
        return "{\n" +
                "    \"Item\": {\n" +
                "        \"active\": {\n" +
                "            \"BOOL\": true\n" +
                "        }," +
                "        \"te\": {\n" +
                "            \"BOOL\": false\n" +
                "        }," +
                "        \"name\": {\n" +
                "            \"S\": \"Leandro\"\n" +
                "        }," +
                "        \"time\": {\n" +
                "            \"N\": \"23.90000\"\n" +
                "        }," +
                "        \"publisher\": {\n" +
                "            \"M\": {\n" +
                "                \"year\": {\n" +
                "                    \"N\": \"2019\"\n" +
                "                },\n" +
                "                \"name\": {\n" +
                "                    \"S\": \"Oreilly\"\n" +
                "                }\n" +
                "            }\n" +
                "        }" +
                "}}";
    }

    /**
     * Returns the total bytes of the element and its nested elements.
     * The calculation includes the rootKey.
     *
     * @param rootKey The root key of the object.
     * @param jsonElement The element to be calculated.
     *
     * @return the total in bytes.
     */
    private int getNestTotalByte(String rootKey, Map.Entry<String, JsonElement> jsonElement) {
        String key = jsonElement.getKey();
        JsonElement value = jsonElement.getValue();
        // check the type of the element
        if (value.isJsonNull()) {
            return 1;
        } else if (value.isJsonPrimitive()) {
            return calcString(rootKey) + calcPrimitive(key, value.getAsJsonPrimitive());
        } else if (value.isJsonObject()) {
            return calcObject(rootKey, jsonElement);
        } else if (value.isJsonArray()) {
            throw new UnsupportedOperationException("Can't calculate list objects yet");
        } else {
            throw new IllegalArgumentException("Format for Json not found");
        }

    }

    /**
     * Returns the total bytes for Map type element and its nested elements.
     * The calculation includes the rootKey.
     *
     * @param rootKey The root key of the object.
     * @param jsonElement The element to be calculated.
     *
     * @return the total in bytes.
     */
    private int calcObject(String rootKey, Map.Entry<String, JsonElement> jsonElement) {
        String key = jsonElement.getKey();

        int totalBytes = 0;
        if (MAP.equals(key)) {
            totalBytes += calcString(rootKey) + 3;
            key = rootKey;
        }
        // iterate nested elements
        for (Map.Entry<String, JsonElement> item : jsonElement.getValue().getAsJsonObject().entrySet()) {
            totalBytes += getNestTotalByte(key, item);
        }
        return totalBytes;
    }

    /**
     * Returns the total bytes of the primitive element. The calculation not include the key.
     * It checks the following types:
     * BOOL: boolean
     * N: Number
     * S: String
     *
     * @param key The key of the element.
     * @param element The element to be calculated.
     * @return the total in bytes.
     */
    private int calcPrimitive(String key, JsonPrimitive element) {
        switch (key) {
            case BOOL: return 1;
            case NUMBER: return calcNumber(element);
            case STRING: return calcString(element.getAsString());
            default: throw new IllegalArgumentException("The value of key [" + key + "] is an incorrect format.");
        }
    }

    /**
     * Returns the total in bytes of the number.
     *
     * @param element the element to be calculated.
     * @return the total in bytes.
     */
    private int calcNumber(JsonPrimitive element) {
        float value = element.getAsFloat();
        if (value == 0) {
            return 1;
        }
        String valueStr = Float.toString(value)
                .replaceAll("\\.?0*$", "")
                .replace(".", "");
        return Math.round(valueStr.length() / 2F) + 1;
    }

    /**
     * The total in bytes with charset UTF-8.
     *
     * @param value the string to be calculated.
     * @return the total in bytes.
     */
    private int calcString(String value) {
        if (value == null) {
            return 0;
        }
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

}
