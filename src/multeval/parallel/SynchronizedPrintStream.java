package multeval.parallel;

import java.io.PrintStream;

/**
 * So here's the use case: You have a compute-heavy task over lines from an
 * ordered file and you'd like to write back the processed lines in order. Let's
 * call each line a "unit" and give it a unit number. This class allows multiple
 * threads to write to the same output file, guaranteeing the proper ordering.
 * 
 * @author Jonathan Clark
 */
public class SynchronizedPrintStream {

	private PrintStream out;
	private int curUnit = 0;
	private Object lock = new Object();

	public SynchronizedPrintStream(PrintStream out) {
		this.out = out;
	}

	public void println(int myUnit, String str) throws InterruptedException {
		while (curUnit != myUnit) {
//			System.err.println("Waiting to write for " + myUnit);
			synchronized (lock) {
				lock.wait();
			}
//			System.err.println("Woke up to check on " + myUnit);
		}
//		System.err.println("Writing for " + myUnit);
		out.println(str);
	}

	public void finishUnit(int unit) {
		synchronized(lock) {
			curUnit += 1;
//			System.err.println("Notifying threads of completion of " + (unit-1));
			lock.notifyAll();
//			System.err.println("Finished notifying threads of completion of " + (unit-1));
		}
	}

	public void close() {
		out.close();
	}
}
