package multeval.parallel;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

import com.google.common.base.Supplier;

public abstract class HypothesisLevelMetricWorkerPool<Task, ThreadLocals> {

	private final int threads;
	private final Task poison;
	private final Supplier<ThreadLocals> threadLocalSupplier;

	private final ArrayList<Thread> workers;
	private final ArrayBlockingQueue<Task> q;

	public HypothesisLevelMetricWorkerPool(int threads, Task poison,
			Supplier<ThreadLocals> threadLocals) {
		this.threads = threads;
		this.poison = poison;
		this.threadLocalSupplier = threadLocals;

		this.workers = new ArrayList<Thread>(threads);
		this.q = new ArrayBlockingQueue<Task>(threads * 10);
	}

	public void start() {
		for (int iThread = 0; iThread < threads; iThread++) {
			Thread thread = new Thread() {
				ThreadLocals locals = threadLocalSupplier.get();

				@Override
				public void run() {
					try {
						Task trip = q.take();
						while (!trip.equals(poison)) {
							doWork(locals, trip);
							trip = q.take();
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
		q.put(t);
	}

	public void waitForCompletion() throws InterruptedException {
		for (int iThread = 0; iThread < threads; iThread++) {
			q.put(poison);
		}

		for (Thread t : workers) {
			t.join();
		}
	}

	public abstract void doWork(ThreadLocals locals, Task t);
}
