package com.personal.loan.outbox;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface KafkaOutboxEventRepository extends JpaRepository<KafkaOutboxEvent, Long> {

    List<KafkaOutboxEvent> findByPublishedFalseOrderByCreatedAtAsc();
}
