package fr.leowenex.hashtransfer.scheduling;

import fr.leowenex.hashtransfer.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class PurgeScheduling {

    private final FileService fileService;

    @Scheduled(cron = "${hashtransfer.expiration-check-cron}")
    public void purgeExpiredFiles() {
        fileService.purgeExpiredFiles();
    }
}
