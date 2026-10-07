package com.careerpilot.repository;

import com.careerpilot.model.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {
    Optional<InterviewSession> findFirstByCandidateIdOrderByStartedAtDesc(Long candidateId);
    List<InterviewSession> findAllByCandidateId(Long candidateId);
}
