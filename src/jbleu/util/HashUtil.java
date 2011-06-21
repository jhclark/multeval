package jbleu.util;

public class HashUtil {
  /* This method was written by Doug Lea with assistance from members of JCP
   * JSR-166 Expert Group and released to the public domain, as explained at
   * http://creativecommons.org/licenses/publicdomain As of 2010/06/11, this
   * method is identical to the (package private) hash method in OpenJDK 7's
   * java.util.HashMap class. It was in turn lifted from Google Guava's Hashing
   * class. */
  static int smear(int hashCode) {
    hashCode ^= (hashCode >>> 20) ^ (hashCode >>> 12);
    return hashCode ^ (hashCode >>> 7) ^ (hashCode >>> 4);
  }
}
