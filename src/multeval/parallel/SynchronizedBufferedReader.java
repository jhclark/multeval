package multeval.parallel;

import java.io.BufferedReader;
import java.io.IOException;

public class SynchronizedBufferedReader {
	private BufferedReader in;

	public SynchronizedBufferedReader(BufferedReader in) {
		this.in = in;
	}
	
	public synchronized String readLine() throws IOException {
		return in.readLine();
	}
	
	public synchronized void close() throws IOException {
		in.close();
	}
}
