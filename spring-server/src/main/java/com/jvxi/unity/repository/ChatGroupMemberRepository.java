package com.jvxi.unity.repository;

import com.jvxi.unity.model.ChatGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatGroupMemberRepository extends JpaRepository<ChatGroupMember, Long> {

    List<ChatGroupMember> findByGroupId(Long groupId);

    Optional<ChatGroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    @Query("SELECT m.groupId FROM ChatGroupMember m WHERE m.userId = :userId")
    List<Long> findGroupIdsByUserId(@Param("userId") Long userId);

    void deleteByGroupIdAndUserId(Long groupId, Long userId);

    long countByGroupId(Long groupId);

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);
}
