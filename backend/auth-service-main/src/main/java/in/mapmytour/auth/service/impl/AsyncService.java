package in.mapmytour.auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncService {

    /**
     * Send email asynchronously
     */
    @Async("communicationExecutor")
    public void sendEmailAsync(Runnable emailTask) {
        try {
            emailTask.run();
        } catch (Exception e) {
            log.error("Async email sending failed", e);
        }
    }

    /**
     * Send SMS asynchronously
     */
    @Async("communicationExecutor")
    public void sendSmsAsync(Runnable smsTask) {
        try {
            smsTask.run();
        } catch (Exception e) {
            log.error("Async SMS sending failed", e);
        }
    }

    /**
     * Process file upload asynchronously
     */
    @Async("taskExecutor")
    public void processFileAsync(Runnable fileTask) {
        try {
            fileTask.run();
        } catch (Exception e) {
            log.error("Async file processing failed", e);
        }
    }
}
