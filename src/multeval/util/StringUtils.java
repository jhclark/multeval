package multeval.util;

import java.util.regex.Pattern;

public class StringUtils {
	
	public static final Pattern WHITESPACE = Pattern.compile("\\s+");
	
	public static String normalizeWhitespace(String sent) {
		return WHITESPACE.matcher(sent.trim()).replaceAll(" ");
	}

}
