package br.com.lnd.testdynamo.size;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Scanner;

/**
 * Calculation of json using the AWS DynamoDB strategy.
 *
 * This class calculates the total size in bytes of a json based on the syntax of a DynamoDB Item.
 * <p>For more information about how DynamoDB calculates items size:</p>
 * @see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/CapacityUnitCalculations.html">DynamoDB Item Sizes</a>
 */
public class ItemSizeCalculation {

    private static final String MAP = "M";
    private static final String BOOL = "BOOL";
    private static final String NUMBER = "N";
    private static final String STRING = "S";

    public static void main(String... args) {
        String json;

        if (args.length == 0) {
            json = getPipeInput();
        } else {
            Path path = Paths.get(args[0]);

            if (Files.isRegularFile(path)) {
                json = getFile(path);
            } else {
                json = args[0];
            }
        }



        System.out.println(new ItemSizeCalculation().calculateInBytes(json));
    }

    private static String getFile(Path path) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = Files.newBufferedReader(path)) {

            // read line by line
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }

        } catch (IOException e) {
            System.err.format("IOException: %s%n", e);
        }

        return sb.toString();
    }

    private static String getPipeInput() {
        StringBuilder json = new StringBuilder();
        Scanner sc = new Scanner(System.in);
        while(sc.hasNextLine()) {
            json.append(sc.nextLine());
        }
        return json.toString();
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
            return calcList(rootKey, value);
        } else {
            throw new IllegalArgumentException("Format for Json not found");
        }
    }

    /**
     * Returns the total bytes for List type element and its nested elements.
     * The calculation includes the rootKey.
     *
     * @param rootKey The root key of the object.
     * @param jsonElement The element to be calculated.
     *
     * @return the total in bytes.
     */
    private int calcList(String rootKey, JsonElement jsonElement) {
        int totalBytes = calcString(rootKey) + 3;
        for (JsonElement elementArray : jsonElement.getAsJsonArray()) {
            for (Map.Entry<String, JsonElement> element : elementArray.getAsJsonObject().entrySet()) {
                totalBytes += getNestTotalByte(null, element);
            }
        }
        return totalBytes;
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
            default: throw new IllegalArgumentException("Incorrect format for value of key [" + key + "]");
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
