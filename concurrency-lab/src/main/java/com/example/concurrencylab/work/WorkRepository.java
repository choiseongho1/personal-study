package com.example.concurrencylab.work;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface WorkRepository extends JpaRepository<WorkItem, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WorkItem w where w.id = :id")
    Optional<WorkItem> lock(@Param("id") String id);

    @Query("""
        select w.id from WorkItem w
        where (w.status = :ready and w.nextAttemptAt <= :now)
           or (w.status = :running and w.leaseUntil <= :now)
        order by w.nextAttemptAt, w.id
        """)
    List<String> findDueStates(@Param("now") Instant now, @Param("ready") WorkItem.Status ready, @Param("running") WorkItem.Status running, Pageable pageable);
    default List<String> findDue(Instant now, Pageable pageable) {
        return findDueStates(now, WorkItem.Status.READY, WorkItem.Status.RUNNING, pageable);
    }
}
