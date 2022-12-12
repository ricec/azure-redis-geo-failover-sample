package sample.azureredis.lettuce.shared;

import java.util.concurrent.*;

public abstract class Example implements AutoCloseable {
    public abstract void executeWrite();

    public void run() throws InterruptedException {
        ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(8);
        int count = 0;
        while(true)
        {
            if (count % 40 == 0) {
                System.out.println("Writing to Redis... (" + executor.getCompletedTaskCount() + " operations completed)");
            }

            for (int i = 0; i < 8; i++) {
                executor.submit(() -> {
                    try
                    {
                        executeWrite();
                    }
                    catch (Exception e)
                    {
                        System.out.println("Failed to write to Redis.");
                        System.out.println(e.getMessage());
                    }
                });
            }

            count++;
            Thread.sleep(50);
        }
    }
}