package com.example.concurrencylab.work;

import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkTransactions {
    public record Claim(String id, WorkItem.Kind kind, Long referenceId, String token) {}
    private final WorkRepository repository;

    @Transactional
    public Optional<Claim> claim(String id) {
        var work = repository.lock(id).orElseThrow();
        if (!work.claimable(Instant.now())) return Optional.empty();
        work.claim(Instant.now());
        return Optional.of(new Claim(id, work.getKind(), work.getReferenceId(), work.getLeaseToken()));
    }
    @Transactional
    public boolean complete(Claim claim) {
        var work = repository.lock(claim.id()).orElseThrow();
        if (!work.ownedBy(claim.token())) return false;
        work.complete();
        return true;
    }
    @Transactional
    public void retry(Claim claim, String error) {
        var work = repository.lock(claim.id()).orElseThrow();
        if (work.ownedBy(claim.token())) work.retry(error, Instant.now());
    }
    @Transactional
    public void review(Claim claim, String error) {
        var work = repository.lock(claim.id()).orElseThrow();
        if (work.ownedBy(claim.token())) work.review(error);
    }
}
