import java.util.ArrayList;
import java.util.List;

/**
 * ProtocolParser
 */
public class ProtocolParser {

    public static String parse(String actualString) {
        CommondType commondType = stringType(actualString);
        var result = switch (commondType) {
            case ARRAY -> parseArray(actualString);
            case STRING -> {
                if (isPing(actualString)) {
                    yield "PONG";
                } else {
                    yield actualString;
                }
            }
            default -> actualString;
        };

        return result;
    }

    private static CommondType stringType(String str) {
        if (str.startsWith("*")) {
            return CommondType.ARRAY;
        } else {
            return CommondType.STRING;
        }
    }

    private static String parseArray(String str) {
        char start = str.charAt(0);
        if (start == '*') {
            String[] arr = new String[99];
            List<String> list = new ArrayList<>();
            arr = str.split("\r\n");
            for (int i = 4; i < arr.length; i = i + 2) {
                list.add(arr[i]);
            }
            String toReturn = String.join(" ", list);
            return toReturn;
        }

        return str;

    }

    private static boolean isPing(String str) {
        return str.contains("ping");
    }
}