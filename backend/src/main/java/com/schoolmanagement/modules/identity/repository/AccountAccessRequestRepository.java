package com.schoolmanagement.modules.identity.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.schoolmanagement.modules.identity.entity.AccountAccessRequest;
import com.schoolmanagement.modules.identity.entity.AccountRequestStatus;
import com.schoolmanagement.modules.identity.entity.UserRole;

public interface AccountAccessRequestRepository extends JpaRepository<AccountAccessRequest, Long> {

    boolean existsByEmailIgnoreCaseAndRequestedRoleAndStatus(
            String email, UserRole requestedRole, AccountRequestStatus status);

    List<AccountAccessRequest> findByStatusOrderByCreatedAtDesc(AccountRequestStatus status);

    List<AccountAccessRequest> findAllByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM AccountAccessRequest r WHERE r.id = :id")
    Optional<AccountAccessRequest> findByIdForUpdate(@Param("id") Long id);
}
