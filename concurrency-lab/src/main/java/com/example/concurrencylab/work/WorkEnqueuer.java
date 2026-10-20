package com.example.concurrencylab.work;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
public class WorkEnqueuer {
    private final WorkRepository repository;

    // 업무 데이터와 같은 트랜잭션에서 작업을 저장하도록 강제합니다.
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(String id, WorkItem.Kind kind, Long referenceId) {
        if (!repository.existsById(id)) {
            repository.save(new WorkItem(id, kind, referenceId));
        }
    }
}
