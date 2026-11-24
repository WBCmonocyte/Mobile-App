package com.easysport.repository;

import com.easysport.entity.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, String> {

    @Query("SELECT t FROM Tournament t JOIN FETCH t.court c WHERE t.startsAt >= :now ORDER BY t.startsAt ASC")
    List<Tournament> findUpcomingTournaments(@Param("now") LocalDateTime now);
}
