package core.utils;

public class NumberUtil {

    public static long flattenLong(long value, int sliceKeepDigits) {
        int digits = countDigits(value);
        if (digits > sliceKeepDigits) {
            return (long) (Math.round(value / Math.pow(10, digits - sliceKeepDigits)) * Math.pow(10, digits - sliceKeepDigits));
        }
        return value;
    }

    public static int countDigits(long value) {
        if (value == 0) {
            return 1;
        }

        int digits = 0;
        while (value != 0) {
            value /= 10;
            digits++;
        }
        return digits;
    }

}
