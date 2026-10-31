package com.ambdulghaffar.springstarterapi.cronJob;

import com.ambdulghaffar.springstarterapi.repository.EmailTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailTokenCleanup {
    private final EmailTokenRepository emailTokenRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purge() {
        int deleted = emailTokenRepository.deleteExpired(Instant.now());
        log.debug("Purged {} expired email tokens", deleted);
    }
}