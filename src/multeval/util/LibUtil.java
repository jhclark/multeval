package multeval.util;

import java.net.*;

public class LibUtil {

  // returns null if not found
  public static void checkLibrary(String qualifiedName, String libName) {
    try {
      Class<?> clazz = Class.forName(qualifiedName);
      URL where = clazz.getProtectionDomain().getCodeSource().getLocation();
      System.err.println("Found library " + libName + " at " + where.toString());
    } catch(ClassNotFoundException e) {
      throw new RuntimeException("Could not find library " + libName, e);
    }
  }

}
