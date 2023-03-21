package pl.mo.planz;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

@Component
public class ScheduledWorker {
    
    DocumentRepository docRepo;

    Thread scheduledThread;
    ScheduledTask scheduledTask;

    @Autowired
    public ScheduledWorker(DocumentRepository docRepo) {

        this.docRepo = docRepo;

        scheduledTask = new ScheduledTask(docRepo, 30000 * 1000);// about a third of a day

        scheduledThread = new Thread(scheduledTask, "schedTask");
        scheduledThread.start();
    }

    @PreDestroy
    public void unschedule() {
        scheduledTask.stop();
        scheduledThread.interrupt();
    }

}

class ScheduledTask implements Runnable {

    long sleepTime;
    int times = 0;
    volatile boolean stop = false;

    DocumentRepository docRepo;

    public ScheduledTask(DocumentRepository docRepo, long sleepTime) {
        this.docRepo = docRepo;
        this.sleepTime = sleepTime;
    }

    public void stop() {
        stop = true;
    }

    @Override
    public void run() {
        int errCount = 0;
        while (true) {
        try {
                System.out.println("Scheduled task: " + times);
                times++;

                // do the everything


            
                Thread.sleep(sleepTime);
            } catch (InterruptedException e){
                System.out.println("Thread interrupted");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                errCount++;
                System.out.println("ScheduledTask exception number: " + errCount);
            }

            if (stop) {
                System.out.println("Thread stopped");
                return;
            }

            if (errCount > 10) {
                break;
            }
        }
        System.out.println("Error count exceeded, stopping");
    }

}

