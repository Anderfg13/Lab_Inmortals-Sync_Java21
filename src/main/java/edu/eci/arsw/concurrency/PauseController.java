package edu.eci.arsw.concurrency;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicInteger;

public final class PauseController {
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition unpaused = lock.newCondition();
  private volatile boolean paused = false;
  
  // new counters for threads in pause 
  private final AtomicInteger threadsPaused = new AtomicInteger(0);
  private final Condition allPaused = lock.newCondition();

  public void pause() { 
    lock.lock(); 
    try { 
      paused = true; 
    } finally { 
      lock.unlock(); 
    } 
  }
  
  public void resume() { 
    lock.lock(); 
    try { 
      paused = false; 
      unpaused.signalAll(); 
    } finally { 
      lock.unlock(); 
    } 
  }
  
  public boolean paused() { 
    return paused; 
  }

  // this help us to now the threads are paused 
  public void reportPaused() {
    threadsPaused.incrementAndGet();
  }
  
  // threads report that they have resumed 
  public void reportResumed() {
    threadsPaused.decrementAndGet();
  }
  
  // wait till every single thread is paused 
  public void awaitAllPaused(int expectedThreads) throws InterruptedException {
    lock.lock();
    try {
        long startTime = System.currentTimeMillis();
        while (threadsPaused.get() < expectedThreads) {
            allPaused.await(100, java.util.concurrent.TimeUnit.MILLISECONDS);
            
            // Timeout después de 5 segundos
            if (System.currentTimeMillis() - startTime > 5000) {
                System.out.println("WARNING: Timeout waiting for all threads to pause");
                System.out.println("Paused: " + threadsPaused.get() + "/" + expectedThreads);
                break;
            }
        }
        System.out.println("All threads paused: " + threadsPaused.get() + "/" + expectedThreads);
    } finally {
        lock.unlock();
      }
  }

  public void awaitIfPaused() throws InterruptedException {
    lock.lockInterruptibly();
    try { 
      while (paused) {
        reportPaused();           // report i am paused 
        allPaused.signal();       // respor if another is paused
        unpaused.await();
        reportResumed();         // Rrepor i resimed 
      }
    }
    finally { 
      lock.unlock(); 
    }
  }
}