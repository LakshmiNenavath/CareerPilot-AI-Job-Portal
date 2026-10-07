package com.careerpilot.repository;

import com.careerpilot.model.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    
    List<Candidate> findAllByOrderByCreatedAtDesc();

    @Query("SELECT c FROM Candidate c WHERE " +
           "LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.detectedRole) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY c.createdAt DESC")
    List<Candidate> searchCandidates(String keyword);

    @Query("SELECT AVG(c.atsScore) FROM Candidate c")
    Double getAverageAtsScore();

    @Query("SELECT AVG(c.interviewScore) FROM Candidate c WHERE c.interviewCompleted = true")
    Double getAverageInterviewScore();

    long countByInterviewCompleted(Boolean interviewCompleted);
}
