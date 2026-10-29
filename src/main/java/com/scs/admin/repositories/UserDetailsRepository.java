package com.scs.admin.repositories;

import com.scs.admin.entities.UserDetails;
import com.scs.admin.entities.UserDetailsId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Optional;

@Repository
public interface UserDetailsRepository extends JpaRepository<UserDetails, UserDetailsId> {

    Optional<UserDetails> findByMobileNumber(String mobileNumber);

    Optional<UserDetails> findByEmailId(String email);

    Optional<UserDetails> findFirstByHierarchyId(String hierarchyId);

    Optional<UserDetails> findByHierarchyId(String hierarchyId);

    @Query("SELECT u FROM UserDetails u WHERE u.userStatus = 'Active' AND u.hierarchyId LIKE %:keyword%")
    Page<UserDetails> searchActiveUsersByHierarchyId(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
        SELECT u
        FROM UserDetails u
        WHERE (
            :keyword IS NULL
            OR TRIM(:keyword) = ''
            OR LOWER(u.hierarchyId) LIKE
                LOWER(CONCAT('%', :keyword, '%'))
            OR u.mobileNumber LIKE
                CONCAT('%', :keyword, '%')
            OR LOWER(u.loginUserName) LIKE
                LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(u.emailId) LIKE
                LOWER(CONCAT('%', :keyword, '%'))
        )
        """)
    Page<UserDetails> findAllUsersWithKeyword(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
        SELECT u
        FROM UserDetails u
        WHERE u.userStatus = 'Active'

          AND UPPER(u.hierarchyCode) IN :allowedHierarchyCodes

          AND (
                :fullAccess = true
                OR u.hierarchyId IN :accessibleIds
          )

          AND (
                :keyword IS NULL
                OR :keyword = ''
                OR LOWER(u.mobileNumber)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.emailId)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.hierarchyId)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.hierarchyCode)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))
          )

          AND (
                :mobileNumber IS NULL
                OR :mobileNumber = ''
                OR LOWER(u.mobileNumber)
                    LIKE LOWER(CONCAT('%', :mobileNumber, '%'))
          )

          AND (
                :hierarchyId IS NULL
                OR :hierarchyId = ''
                OR LOWER(u.hierarchyId)
                    LIKE LOWER(CONCAT('%', :hierarchyId, '%'))
          )

          AND (
                :hierarchyCode IS NULL
                OR :hierarchyCode = ''
                OR LOWER(u.hierarchyCode)
                    LIKE LOWER(CONCAT('%', :hierarchyCode, '%'))
          )

          AND (
                :emailId IS NULL
                OR :emailId = ''
                OR LOWER(u.emailId)
                    LIKE LOWER(CONCAT('%', :emailId, '%'))
          )

          AND (
                :createdDate IS NULL
                OR :createdDate = ''
                OR CAST(u.createdDate AS string)
                    LIKE CONCAT('%', :createdDate, '%')
          )

          AND (
                :lastLoginDate IS NULL
                OR :lastLoginDate = ''
                OR CAST(u.lastLoginDate AS string)
                    LIKE CONCAT('%', :lastLoginDate, '%')
          )
        """)
    Page<UserDetails> searchActiveHierarchies(

            @Param("allowedHierarchyCodes")
            List<String> allowedHierarchyCodes,

            @Param("fullAccess")
            boolean fullAccess,

            @Param("accessibleIds")
            List<String> accessibleIds,

            @Param("keyword")
            String keyword,

            @Param("mobileNumber")
            String mobileNumber,

            @Param("hierarchyId")
            String hierarchyId,

            @Param("hierarchyCode")
            String hierarchyCode,

            @Param("createdDate")
            String createdDate,

            @Param("lastLoginDate")
            String lastLoginDate,

            @Param("emailId")
            String emailId,

            Pageable pageable
    );

    @Query("SELECT u.hierarchyId FROM UserDetails u WHERE u.hierarchyCode = :code")
    List<String> findHierarchyIdsByHierarchyCode(@Param("code") String code);

    @Query("SELECT u FROM UserDetails u WHERE u.hierarchyId = :value OR u.mobileNumber = :value")
    UserDetails findByHierarchyIdOrMobileNumber(@Param("value") String value);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserDetails u WHERE u.hierarchyId = :hierarchyId")
    void deleteByHierarchyId(@Param("hierarchyId") String hierarchyId);

    List<UserDetails> findByParentAccount(String parentId);

    @Query("""
            SELECT u
            FROM UserDetails u
            WHERE u.hierarchyId IN (
                SELECT p.id.descendantId
                FROM AccessHierarchyPath p
                WHERE p.id.ancestorId = :ancestorId
            )
            AND (
                :keyword IS NULL
                OR :keyword = ''
                OR LOWER(u.hierarchyId) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR u.mobileNumber LIKE CONCAT('%', :keyword, '%')
            )
            """)
    Page<UserDetails> findAccessibleUsers(
            @Param("ancestorId") String ancestorId,
            @Param("keyword") String keyword,
            Pageable pageable);

    List<UserDetails> findAllByHierarchyIdIgnoreCase(String hierarchyId);

    List<UserDetails> findAllByMobileNumber(String mobileNumber);

    @Query("""
        SELECT u
        FROM UserDetails u
        WHERE u.hierarchyId IN :hierarchyIds
          AND u.userStatus = 'Active'
        ORDER BY u.hierarchyCode ASC, u.hierarchyId ASC
        """)
    List<UserDetails> findActiveUsersByHierarchyIds(
            @Param("hierarchyIds") List<String> hierarchyIds
    );

}
