package com.schoolmanagement.modules.identity.repository;

import java.util.List; 

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.schoolmanagement.modules.identity.entity.GuardianProfile;

public interface GuardianProfileRepository
        extends JpaRepository<GuardianProfile, Long> {

    List<GuardianProfile> findAllByOrderByIdAsc();
    
    @Query("""
    	    SELECT g
    	    FROM GuardianProfile g, AppUser u
    	    WHERE g.userId = u.id
    	      AND LOWER(u.email) = LOWER(:email)
    	    """)
    	Optional<GuardianProfile> findByUserEmail(
    	        @Param("email") String email
    	);
}