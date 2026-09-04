package com.example.schedule.controller;

import com.example.schedule.service.DeadLetterService;
import com.example.schedule.repository.JobDeadLetterRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/dead-letters")
public class DeadLetterController {

    private final JobDeadLetterRepository repository;
    private final DeadLetterService deadLetterService;

    public DeadLetterController(JobDeadLetterRepository repository, DeadLetterService deadLetterService) {
        this.repository = repository;
        this.deadLetterService = deadLetterService;
    }

    @GetMapping
    public List<?> listUnresolved() {
        return repository.findByResolvedFalse();
    }

    @PostMapping("/{id}/replay")
    public String replay(@PathVariable Long id, @RequestParam String operator) {
        deadLetterService.replay(id, operator);
        return "Replayed successfully by " + operator;
    }
}