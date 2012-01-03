package multeval.parallel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;

public abstract class MetricWorkerPool<Task, ThreadLocals> {

	private final int threads;
	private final Supplier<ThreadLocals> threadLocalSupplier;
	private final int chunkSize;
	
	private final List<Task> POISON = ImmutableList.of();
	private List<Task> curChunk;
	private final ArrayList<Thread> workers;
	private final ArrayBlockingQueue<List<Task>> q;

	// usually 100 hypotheses or bootstrap points at a time
	public static final int DEFAULT_CHUNK_SIZE = 100;
	
	public MetricWorkerPool(int threads, Supplier<ThreadLocals> threadLocals) {
		this(threads, threadLocals, DEFAULT_CHUNK_SIZE);
	}
	
	// chunk size is used to reduce contention among threads
	public MetricWorkerPool(int threads, Supplier<ThreadLocals> threadLocals, int chunkSize) {
		
		this.threads = threads;
		this.threadLocalSupplier = threadLocals;
		this.chunkSize = chunkSize;

		this.curChunk = new ArrayList<Task>(chunkSize);
		this.workers = new ArrayList<Thread>(threads);
		this.q = new ArrayBlockingQueue<List<Task>>(threads * 100);
	}

	public void start() {
		for (int iThread = 0; iThread < threads; iThread++) {
			Thread thread = new Thread() {
				ThreadLocals locals = threadLocalSupplier.get();

				@Override
				public void run() {
					try {
						List<Task> chunk = q.take();
						while (!chunk.equals(POISON)) {
							for(Task task : chunk) {
								doWork(locals, task);
							}
							chunk = q.take();
						}
					} catch (InterruptedException e) {
						;
					} catch (Throwable t) {
						t.printStackTrace();
						System.exit(1);
					}
				}
			};
			workers.add(thread);
			thread.start();
		}
	}

	public void addTask(Task t) throws InterruptedException {
		if(curChunk.size() == chunkSize) {
			q.put(curChunk);
			curChunk = new ArrayList<Task>(chunkSize);
		}
		curChunk.add(t);
	}

	public void waitForCompletion() throws InterruptedException {
		if(curChunk.size() > 0) {
			q.put(curChunk);
		}
		
		for (int iThread = 0; iThread < threads; iThread++) {
			q.put(POISON);
		}

		for (Thread t : workers) {
			t.join();
		}
	}

	public abstract void doWork(ThreadLocals locals, Task t);
}
