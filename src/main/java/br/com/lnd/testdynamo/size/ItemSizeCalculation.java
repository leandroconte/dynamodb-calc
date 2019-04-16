package br.com.lnd.testdynamo.size;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

public class ItemSizeCalculation {

    public static void main(String[] args) {
        JsonObject asJsonObject = new JsonParser().parse("{\n" +
                "    \"Item\": {\n" +
                "        \"active\": {\n" +
                "            \"BOOL\": true\n" +
                "        },"+
                "        \"te\": {\n" +
                "            \"BOOL\": false\n" +
                "        }," +
                "        \"name\": {\n" +
                "            \"S\": \"Leandro\"\n" +
                "        }," +
                "        \"time\": {\n" +
                "            \"N\": \"23.90000\"\n" +
                "        }" +
                "}" +
                "}").getAsJsonObject();
        JsonObject rootItem = asJsonObject.getAsJsonObject("Item");
        int totalBytes = 0;
        for (Map.Entry<String, JsonElement> item : rootItem.entrySet()) {
            totalBytes += getNestTotalByte(null, item);
//            System.out.println(totalBytes);
        }
        System.out.println("Total de bytes: " + totalBytes);
    }

    private static int getNestTotalByte(String beforeKey, Map.Entry<String, JsonElement> jsonElement) {
        String key = jsonElement.getKey();
        JsonElement value = jsonElement.getValue();

        if (value.isJsonNull()) {
            return 1;
        } else if (value.isJsonPrimitive()) {
            return calcString(beforeKey) + calcPrimitive(key, value.getAsJsonPrimitive());
        } else if (value.isJsonObject()) {
            return calcMap(beforeKey, jsonElement);
        } else if (value.isJsonArray()) {
            return 0;
        } else {
            throw new IllegalArgumentException("Format for Json not found");
        }

    }

//    private static int calcMapOrList(String key, Map.Entry<String, JsonElement> jsonElement) {
//        if (jsonElement.getValue().isJsonArray()) {
//            return calcList(key, jsonElement);
//        }
//        return calcMap(key, jsonElement);
//    }

    private static int calcPrimitive(String key, JsonPrimitive element) {
        switch (key) {
            case "BOOL": return calcBoolean();
            case "N": return calcNumber(element);
            case "S": return calcString(element.getAsString());
            default: throw new IllegalArgumentException("The value of key [" + key + "] is an incorrect format.");
        }
    }

//    private static int calcList(String key, Map.Entry<String, JsonElement> jsonElement) {
//        JsonArray asJsonArray = jsonElement.getValue().getAsJsonArray();
//
//        int totalByte = 0;
//        for (JsonElement element : asJsonArray) {
//            if (element.isJsonObject()) {
//                for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
//                    totalByte += calcMap(key, entry);
//                }
//            } else {
//
//            }
//
//
//            totalByte += getNestTotalByte(jsonElement);
//        }
//        return totalByte;
//    }

    private static int calcMap(String key, Map.Entry<String, JsonElement> jsonElement) {
        int totalNested = 3;
        for (Map.Entry<String, JsonElement> element : jsonElement.getValue().getAsJsonObject().entrySet()) {
            totalNested += getNestTotalByte(element.getKey(), element);
        }
        return totalNested + calcString(key);
    }

    private static int calcMapOrList(Collection collection) {
        int total = 0;
        collection.forEach(item -> {

        });
        return total + 3;
    }

    private static int calcBoolean() {
        return 1;
    }

    private static int calcNumber(JsonPrimitive element) {
        float value = element.getAsFloat();
        if (value == 0) {
            return 1;
        }
        String valueStr = Float.toString(value)
                .replaceAll("\\.?0*$", "")
                .replace(".", "");
        return Math.round(valueStr.length() / 2F) + 1;
    }

    private static int calcString(String value) {
        if (value == null) {
            return 0;
        }
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

}
