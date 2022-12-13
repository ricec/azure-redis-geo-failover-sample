package sample.azureredis.lettuce.shared;

import java.util.concurrent.*;

public abstract class Example implements AutoCloseable {
    public abstract void executeWrite();

    public void run() throws InterruptedException {
        BlockingQueue q = new ArrayBlockingQueue(1000);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(8, 8, 20, TimeUnit.SECONDS, q);
        
        int count = 0;
        int rejectedCount = 0;
        while(true)
        {
            if (count % 500 == 0) {
                System.out.printf("Writing to Redis... (Active: %d, Queued: %d, Completed: %d, Rejected: %d)\n",
                    executor.getActiveCount(),
                    executor.getQueue().size(),
                    executor.getCompletedTaskCount(),
                    rejectedCount);
            }

            try {
                executor.submit(() -> {
                    try {
                        executeWrite();
                    }
                    catch (Exception e) {
                        System.out.println("Failed to write to Redis.");
                        System.out.println(e.getMessage());
                    }
                });
            } catch (RejectedExecutionException e) {
                rejectedCount++;
            }
            
            Thread.sleep(8);
            count++;
        }
    }
}