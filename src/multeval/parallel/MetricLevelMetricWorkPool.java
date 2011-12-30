package multeval.parallel;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import multeval.metrics.Metric;

public abstract class MetricLevelMetricWorkPool {

	private final List<Metric<?>> metrics;
	private final ExecutorService pool;

	public MetricLevelMetricWorkPool(List<Metric<?>> metrics, int threads) {
		this.metrics = metrics;				
		this.pool = Executors.newFixedThreadPool(threads);
	}

	public void beginMetric(Metric<?> metric) {
		System.err.println("Collecting sufficient statistics for metric: "
				+ metric.toString());
	}

	public void finishMetric(Metric<?> metric) {
		System.err.println("Finished collecting sufficient statistics for metric: "
				+ metric.toString());
	}

	public void start() throws InterruptedException {
		for (int iMetric = 0; iMetric < metrics.size(); iMetric++) {
			final Metric<?> metric = metrics.get(iMetric);
			if (!metric.isThreadsafe()) {
				beginMetric(metric);
				final int iMetricF = iMetric;
				
				Runnable task = new Runnable() {
					public void run() {
						try {
							doWork(iMetricF, metric);
							finishMetric(metric);
						} catch (Throwable t) {
							t.printStackTrace();
							System.exit(1);
						}
					}
				};
				pool.execute(task);
			}
		}
	}

	public abstract void doWork(int iMetric, Metric<?> metric);

	public void waitForCompletion() throws InterruptedException {
		pool.shutdown();
		pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
	}
}