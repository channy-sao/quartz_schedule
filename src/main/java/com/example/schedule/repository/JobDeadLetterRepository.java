package com.example.schedule.repository;

import com.example.schedule.entity.JobDeadLetter;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobDeadLetterRepository extends JpaRepository<JobDeadLetter, Long> {
    List<JobDeadLetter> findByResolvedFalse();
}