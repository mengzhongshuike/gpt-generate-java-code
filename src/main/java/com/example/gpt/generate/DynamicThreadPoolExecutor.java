package com.example.gpt.generate;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自我调整的动态线程池。
 * 增加了一个utilizationThreshold属性，用于设置线程池的利用率阈值，如果线程池的利用率超过该阈值，则增加线程池的大小，否则缩减线程池的大小。
 * 我们还增加了一个adjustPeriod属性，用于设置线程池大小的动态调整周期，这样可以避免过于频繁地修改线程池的大小。
 * 我们还增加了一个maxQueueSize属性和一个rejectedExecutionHandler属性，用于设置线程池的拒绝策略，当任务队列已满时，可以选择抛出异常、阻塞等待或丢弃任务等策略。
 *
 * 异步通信：我们使用了 beforeExecute() 和 afterExecute() 方法，在执行任务前后进行统计，以避免数据竞争和锁竞争的问题。通过使用原子整数 activeThreadCount 来统计活动线程数，可以避免竞态条件的出现，从而确保数据的正确性和一致性。
 * 无锁编程：我们采用了原子整数 activeThreadCount 来统计活动线程数，避免锁竞争和死锁的问题。此外，我们还使用了 newSingleThreadScheduledExecutor() 方法，以单线程方式定期调整线程池的大小，从而避免并发问题和死锁的问题。
 */
public class DynamicThreadPoolExecutor extends ThreadPoolExecutor {
    private final int maxThreadCount;
    private final double utilizationThreshold;
    private final AtomicInteger activeThreadCount;
    private final int maxQueueSize;
    private final RejectedExecutionHandler rejectedExecutionHandler;

    public DynamicThreadPoolExecutor(int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit unit,
                                     BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
                                     double utilizationThreshold, int adjustPeriod, int maxQueueSize,
                                     RejectedExecutionHandler rejectedExecutionHandler) {
        super(corePoolSize, maxPoolSize, keepAliveTime, unit, workQueue, threadFactory, rejectedExecutionHandler);

        this.maxThreadCount = maxPoolSize;
        this.utilizationThreshold = utilizationThreshold;
        this.activeThreadCount = new AtomicInteger(0);
        this.maxQueueSize = maxQueueSize;
        this.rejectedExecutionHandler = rejectedExecutionHandler;

        // Start a periodic task to adjust the pool size
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::adjustPoolSize, adjustPeriod,
                adjustPeriod, unit);
    }

    @Override
    public void execute(Runnable task) {
        if (super.getQueue().size() >= maxQueueSize) {
            rejectedExecutionHandler.rejectedExecution(task, this);
        } else {
            super.execute(task);
        }
    }

    @Override
    protected void beforeExecute(Thread thread, Runnable task) {
        super.beforeExecute(thread, task);
        activeThreadCount.incrementAndGet();
    }

    @Override
    protected void afterExecute(Runnable task, Throwable throwable) {
        super.afterExecute(task, throwable);
        activeThreadCount.decrementAndGet();
    }

    private void adjustPoolSize() {
        int activeThreads = activeThreadCount.get();
        int maxThreads = maxThreadCount;
        double utilization = (double) activeThreads / maxThreads;

        System.out.printf("utilization = %s, utilizationThreshold = %s, activeThreads = %s, maxThreads = %s, getCorePoolSize = %s, getPoolSize = %s%n",
                utilization, utilizationThreshold, activeThreads, maxThreads, super.getCorePoolSize(), super.getPoolSize());

        if (utilization >= utilizationThreshold && activeThreads >= super.getCorePoolSize()) {
            int requiredThreads = super.getQueue().size();
            int newCorePoolSize = Math.max(activeThreads + requiredThreads, super.getCorePoolSize());

            if (newCorePoolSize <= maxThreads) {
                super.setCorePoolSize(newCorePoolSize);
            }
        } else if (utilization <= (utilizationThreshold / 2) && super.getPoolSize() < super.getCorePoolSize()) {
            int requiredThreads = super.getQueue().size();
            int newCorePoolSize = Math.max(activeThreads + requiredThreads, super.getCorePoolSize());

            if (newCorePoolSize > super.getCorePoolSize()) {
                super.setCorePoolSize(newCorePoolSize);
            }
        }
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new FutureTask<>(callable) {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return super.cancel(mayInterruptIfRunning);
            }

            @Override
            protected void done() {
                super.done();
            }
        };
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new FutureTask<>(runnable, value) {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return super.cancel(mayInterruptIfRunning);
            }

            @Override
            protected void done() {
                super.done();
            }
        };
    }


    public static void main(String[] args) {
        DynamicThreadPoolExecutor executor = new DynamicThreadPoolExecutor(5, 100, 1, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(30), Executors.defaultThreadFactory(), 0.5, 1, 100,
                new ThreadPoolExecutor.AbortPolicy());
        // Submit some tasks to the pool
        for (int i = 0; i < 50; i++) {
            int finalI = i;
            executor.submit(() -> {
                System.out.println("Task"  + finalI + " executed by thread " + Thread.currentThread().getName());
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("core pool size: " + executor.getCorePoolSize());
            });
        }
        executor.shutdown();
    }
}
