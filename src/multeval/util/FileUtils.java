package multeval.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.google.common.base.Charsets;

public class FileUtils {

	// File must be ASCII or UTF-8!
	public static String getLastLine(String file) throws IOException {
		RandomAccessFile ra = new RandomAccessFile(new File(file), "r");
		long pos = ra.length()-2; // skip newline at end of file
		while( ((char)ra.readByte()) != '\n' && pos > 0) {
			pos--; 
			ra.seek(pos);
		}
		int len = (int) (ra.length() - ra.getFilePointer());
		byte[] buf = new byte[len];
		ra.readFully(buf);
		String str = new String(buf, Charsets.UTF_8);
		return str;
	}
	
	
	public static void main(String[] args) throws IOException {
		System.err.println(getLastLine(args[0]));
	}

}
