package com.scs.admin.repositories;

import com.scs.admin.entities.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {
}
