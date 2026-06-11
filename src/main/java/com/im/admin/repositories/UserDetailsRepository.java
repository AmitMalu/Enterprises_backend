package com.im.admin.repositories;

import com.im.admin.entities.UserDetails;
import com.im.admin.entities.UserDetailsId;
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

    @Query("SELECT u FROM UserDetails u " +
            "WHERE u.userStatus = 'Active' " +
            "AND u.hierarchyCode = 'RT' " +
            "AND (" +
            "   :keyword IS NULL OR :keyword = '' OR " +
            "   LOWER(u.mobileNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "   LOWER(u.emailId) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "   LOWER(u.hierarchyId) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
            ")")
    Page<UserDetails> searchActiveRetailers(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT u.hierarchyId FROM UserDetails u WHERE u.hierarchyCode = :code")
    List<String> findHierarchyIdsByHierarchyCode(@Param("code") String code);

    @Query("SELECT u FROM UserDetails u WHERE u.hierarchyId = :value OR u.mobileNumber = :value")
    UserDetails findByHierarchyIdOrMobileNumber(@Param("value") String value);

    Optional<UserDetails> findByLoginUserName(String loginUserName);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserDetails u WHERE u.hierarchyId = :hierarchyId")
    void deleteByHierarchyId(@Param("hierarchyId") String hierarchyId);

    List<UserDetails> findByParentAccount(String parentId);

//    @Query("""
//       SELECT u
//       FROM UserDetails u
//       WHERE u.hierarchyId IN (
//           SELECT p.id.descendantId
//           FROM AccessHierarchyPath p
//           WHERE p.id.ancestorId = :loginHierarchyId
//       )
//       """)
//    Page<UserDetails> findAccessibleUsers(
//            @Param("loginHierarchyId") String loginHierarchyId,
//            Pageable pageable);

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

}
