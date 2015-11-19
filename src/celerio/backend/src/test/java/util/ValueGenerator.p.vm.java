## Copyright 2015 JAXIO http://www.jaxio.com
##
## Licensed under the Apache License, Version 2.0 (the "License");
## you may not use this file except in compliance with the License.
## You may obtain a copy of the License at
##
##    http://www.apache.org/licenses/LICENSE-2.0
##
## Unless required by applicable law or agreed to in writing, software
## distributed under the License is distributed on an "AS IS" BASIS,
## WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
## See the License for the specific language governing permissions and
## limitations under the License.
##
$output.javaTest($Util, "ValueGenerator")##

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generate values for database tests
 */
public class $output.currentClass {

    /**
     * Get a min date
     *
     * @return the min date
     */
    public static Date getMinDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(1970, 1, 1, 1, 1, 1);
        return cal.getTime();
    }

    /**
     * Get a max date
     *
     * @return the max date
     */
    public static Date getMaxDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(2036, 12, 28, 23, 59, 59);
        return cal.getTime();
    }

    private static final String MAX_STRING = "ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ";

    /**
     * Use for fill the bdd
     * @param size
     * @return a long dummy string
     */
    public static String getMaxString(int size) {
        return MAX_STRING.substring(0, Math.min(size, MAX_STRING.length()));
    }

    private static final int NUM_CHARS = 32;
    private static int[] lastString = new int[NUM_CHARS];
    private static final String CHARS = "abcdefghijklmonpqrstuvwxyz";

    /**
     * Get a unique string
     *
     * @return the unique string
     */
    private static String getUniqueString() {
        char[] buf = new char[NUM_CHARS];

        carry(lastString, buf.length - 1);
        for (int i = 0; i < buf.length; i++) {
            buf[i] = CHARS.charAt(lastString[i]);
        }
        return new String(buf);
    }

    private static void carry(int[] ca, int index) {
        if (ca[index] == (CHARS.length() - 1)) {
            ca[index] = 0;
            carry(ca, --index);
        } else {
            ca[index] = ca[index] + 1;
        }
    }

    /**
     * Get a unique string with length < maxLength
     *
     * @return the unique string
     */
    public static String getUniqueString(int maxLength) {
        if (maxLength == 1) {
            return String.valueOf(getUniqueChar());
        }

        if (maxLength < NUM_CHARS) {
            return getUniqueString().substring(NUM_CHARS - maxLength);
        }
        return getUniqueString();
    }

    private static Calendar uniqueCal;

    /**
     * Get a unique date
     * start in 1970 and day auto increment
     * @return the unique date
     */
    synchronized public static Date getUniqueDate() {
        if (uniqueCal == null) {
            uniqueCal = Calendar.getInstance();
            uniqueCal.set(1970, 1, 1, 1, 1, 1);
        }
        uniqueCal.add(Calendar.DAY_OF_MONTH, 1);
        return uniqueCal.getTime();
    }

    /**
     * Get a unique emain
     * @return the unique mail
     */
    public static String getUniqueEmail() {
        return "email" + getUniqueString(6) + "-" + getUniqueString(10) + "@domain" + getUniqueString(10) + ".com";
    }

    /**
     * Get a unique bytes
     * @return the unique bytes
     */
    public static byte[] getUniqueBytes(int maxSize) {
        return getUniqueString(maxSize).getBytes();
    }

    private static AtomicInteger counter = new AtomicInteger();

    public static Integer getUniqueInteger() {
        return counter.incrementAndGet();
    }

    public static Long getUniqueLong() {
        return new Long(counter.incrementAndGet());
    }

    public static Float getUniqueFloat() {
        return new Float(counter.incrementAndGet());
    }

    public static Double getUniqueDouble() {
        return new Double(counter.incrementAndGet());
    }

    public static BigInteger getUniqueBigInteger() {
        return new BigInteger("" + counter.incrementAndGet());
    }

    public static BigDecimal getUniqueBigDecimal() {
        return new BigDecimal(counter.incrementAndGet());
    }

    /**
     * Get a unique char
     *
     * @return the unique char
     */
    public static char getUniqueChar() {
        return CHARS.charAt(getNextPosition());
    }

    /**
     * Get a unique byte
     * @return the unique byte
     */
    public static byte getUniqueByte() {
        return (byte) getUniqueChar();
    }

    private static AtomicInteger charPosition = new AtomicInteger();

    private static int getNextPosition() {
        return charPosition.incrementAndGet() % CHARS.length();
    }
}