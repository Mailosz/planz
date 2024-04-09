package pl.mo.planz;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import pl.mo.planz.controllers.Controller;
import pl.mo.planz.repositories.DocumentRepository;
import pl.mo.planz.repositories.FieldValueHistoryRepository;
import pl.mo.planz.repositories.FieldValueRepository;
import pl.mo.planz.repositories.TemplateRepository;

@Component
public class ScheduledWorker {
    
    DocumentRepository docRepo;

    Thread scheduledThread;
    ScheduledTask scheduledTask;

    @Autowired
    public ScheduledWorker(DocumentRepository docRepo, TemplateRepository templateRepo, FieldValueRepository valueRepo, FieldValueHistoryRepository histRepo) {

        this.docRepo = docRepo;

        scheduledTask = new ScheduledTask(docRepo, templateRepo, valueRepo, histRepo, 30000 * 1000);// about a third of a day

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
    TemplateRepository templateRepo;
    FieldValueRepository valueRepo;
    FieldValueHistoryRepository histRepo;

    public ScheduledTask(DocumentRepository docRepo, TemplateRepository templateRepo, FieldValueRepository valueRepo, FieldValueHistoryRepository histRepo, long sleepTime) {
        this.docRepo = docRepo;
        this.templateRepo = templateRepo;
        this.valueRepo = valueRepo;
        this.histRepo = histRepo;
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

                // Persistence.createEntityManagerFactory

                // RepositoryFactorySupport rfs = new JpaRepositoryFactory(null);
                


                // DocumentRepository docRepo = rfs.getRepository(DocumentRepository.class);
                // TemplateRepository templateRepo = rfs.getRepository(TemplateRepository.class);
                // FieldValueRepository valueRepo = rfs.getRepository(FieldValueRepository.class);
                // FieldValueHistoryRepository histRepo = rfs.getRepository(FieldValueHistoryRepository.class);

                // do the everything

                // computing current document
                UUID currentDocId = Controller.findCurrentDocument(docRepo);
                if (Controller.currentDocumentId == null) {
                    System.out.println("Current document id null, setting");
                    Controller.currentDocumentId = currentDocId;
                } else {
                    if (Controller.currentDocumentId.equals(currentDocId)) {
                        System.out.println("Current document id same");
                    } else {
                        System.out.println("Current different, changing");
                        Controller.currentDocumentId = currentDocId;
                    }
                }
                System.out.println("Current document id: " + currentDocId.toString());

                //generating documents
                //DocumentGenerator.generateDocuments(docRepo, templateRepo);
                errCount = 0;  // przynajmniej część się wykonała, nie chcemy przerywać threada
                DocumentGenerator.removeOldDocuments(docRepo, valueRepo, histRepo);
            
                Thread.sleep(sleepTime);
                errCount = 0; // reset err counter if no error
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

            if (errCount > 0) {
                if (errCount % 2 == 0) {
                    try {
                        Thread.sleep(600000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();

                        if (stop) {
                            System.out.println("Thread stopped");
                            return;
                        }
                    }
                }
                if (errCount > 10) {
                    break;
                }
            }
        }
        System.out.println("Error count exceeded, stopping");
    }

}

