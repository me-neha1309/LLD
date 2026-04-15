import model.User;
import model.UserTier;
import service.RateLimiterService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    static void checkConcurrency(RateLimiterService service) throws InterruptedException {
        System.out.println("=== STRESS CONCURRENCY TEST ===");

        User user = new User("concurrent-user", UserTier.PREMIUM);
        int threads = 20;
        int requestsPerThread = 10; // total = 200

        ExecutorService executor = Executors.newFixedThreadPool(threads);

        AtomicInteger allowedCount = new AtomicInteger(0);
        AtomicInteger deniedCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    boolean allowed = service.allowRequest(user);

                    if (allowed) {
                        allowedCount.incrementAndGet();
                    } else {
                        deniedCount.incrementAndGet();
                    }

                    System.out.println(Thread.currentThread().getName()
                            + " request: " + (allowed ? "ALLOWED" : "DENIED"));
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        System.out.println("\n=== FINAL COUNT ===");
        System.out.println("ALLOWED: " + allowedCount.get());
        System.out.println("DENIED : " + deniedCount.get());
        System.out.println("TOTAL  : " + (allowedCount.get() + deniedCount.get()));
    }


    public static void main(String[] args) throws InterruptedException {
        RateLimiterService rateLimiterService = new RateLimiterService();

        User freeUser = new User("user1", UserTier.FREE);
        User premiumUser = new User("user2", UserTier.PREMIUM);

       System.out.println("=== FREE USER REQUESTS ===");

        for(int i=1; i<=15; i++){
            boolean allowed = rateLimiterService.allowRequest(freeUser);
            System.out.println("Request " + i + " For Free User: " + (allowed ? "ALLOWED" : "DENIED"));
            Thread.sleep(100);
        }

        System.out.println("=== PREMIUM USER REQUESTS ===");
        for(int i=1; i<=120; i++){
            boolean allowed = rateLimiterService.allowRequest(premiumUser);
            System.out.println("Request " + i + " For Premium User: " + (allowed ? "ALLOWED" : "DENIED"));
            Thread.sleep(100);
        }

        //checkConcurrency(rateLimiterService);
    }
}
