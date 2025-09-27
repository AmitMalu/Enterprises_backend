package com.im.admin.service.impl;

import com.im.admin.dto.*;
import com.im.admin.entities.Ticket;
import com.im.admin.entities.TicketComment;
import com.im.admin.entities.UserDetails;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.repositories.TicketCommentRepository;
import com.im.admin.repositories.TicketRepository;
import com.im.admin.repositories.UserDetailsRepository;
import com.im.admin.service.TicketService;
import com.im.admin.spec.GenericSpecification;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.ImageHandlerUtil;
import com.im.admin.util.ImageUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.fasterxml.jackson.datatype.jsr310.DecimalUtils.toBigDecimal;

@Service
@Slf4j
public class TicketServiceImpl implements TicketService {

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketCommentRepository ticketCommentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private ImageHandlerUtil imageHandlerUtil;

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private ImageUtil imageUtil;




    @Override
    public Object getTransactionDetails(String transactionId, String hierarchyId) {
        String hierarchyType = commonMethods.extractHierarchyType(hierarchyId);
        String entityName = commonMethods.getEntityName(hierarchyType); // e.g., "retailer"

        WalletType[] walletTypes = {WalletType.PREPAID, WalletType.POSTPAID};


        for (WalletType walletType : walletTypes) {
            String tableName = entityName + "_" + walletType.name().toLowerCase() + "_transaction_details";
            String sql = "SELECT * FROM " + tableName + " WHERE transaction_id = :transactionId";

            try {
                Query query = entityManager.createNativeQuery(sql, Map.class);
                query.setParameter("transactionId", transactionId);
                Object result = query.getSingleResult();

                System.out.println("Found in table: " + tableName); // Optional logging
                return result;
            } catch (NoResultException e) {
                // No result, try next table
            }
        }

        // If no record found in either table
        throw new RuntimeException("No record found for transactionId: " + transactionId);
    }


    @Override
    public void addComment(AddTicketCommentDto dto) {

        Ticket ticket = ticketRepository.findById(dto.getTicketId())
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        TicketComment comment = new TicketComment();
        comment.setTicket(ticket);
        comment.setCommentText(dto.getCommentText());
        comment.setAddedBy(dto.getAddedBy());

        ticketCommentRepository.save(comment);
    }

    @Override
    public void updateStatus(UpdateTicketStatusDto dto) {

        Ticket ticket = ticketRepository.findById(dto.getTicketId())
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        ticket.setStatus(dto.getNewStatus());
        ticket.setUpdatedAt(LocalDateTime.now());


        // Add internal comment if resolved
        if ("RESOLVED".equalsIgnoreCase(dto.getNewStatus())) {
            TicketComment resolvedComment = new TicketComment();
            resolvedComment.setTicket(ticket);
            resolvedComment.setCommentText("Ticket marked as RESOLVED.");
            resolvedComment.setAddedBy("SYSTEM"); // or "ADMIN"
            resolvedComment.setCreatedAt(LocalDateTime.now());

            ticket.getComments().add(resolvedComment);
        }

        ticketRepository.save(ticket);
    }

    @Override
    public Ticket getTicketWithComments(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
    }

    @Override
    public Ticket saveTicket(RaiseTicketRequestDto request, MultipartFile screenshotImage) {
        Ticket ticket = new Ticket();
        ticket.setTransactionId(request.getTransactionId());
        ticket.setIssueType(request.getIssueType());
        ticket.setDescription(request.getDescription());
        ticket.setEmail(request.getEmail());
        ticket.setMobileNumber(request.getMobileNumber());
        ticket.setSubProductName(request.getSubProductName());
        ticket.setStatus("PENDING");

        // Fetch UserDetails and set raisedBy from hierarchyId
        Optional<UserDetails> optionalUser = userDetailsRepository.findByMobileNumber(request.getMobileNumber());
        if (optionalUser.isPresent()) {
            UserDetails user = optionalUser.get();
            if (user.getHierarchyId() != null) {
                ticket.setRaisedBy(user.getHierarchyId());
            } else {
                throw new RuntimeException("Hierarchy ID not found for user with mobile number: " + request.getMobileNumber());
            }
        } else {
            throw new RuntimeException("User not found with mobile number: " + request.getMobileNumber());
        }

        try {
            String imagePath = imageUtil.saveImage(screenshotImage, "Ticket");
            ticket.setScreenshotImage(imagePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload screenshot image", e);
        }

        return ticketRepository.save(ticket);
    }



    @Override
    public List<TicketIssueTypeDto> getAllIssueTypes() {
        List<Ticket> allIssues = ticketRepository.findAll();
        return allIssues.stream()
                .map(issue -> new TicketIssueTypeDto(issue.getId(), issue.getIssueType()))
                .toList();
    }

    @Override
    public Page<TicketWithTransactionDto> getAllTickets(Map<String, Object> filters, Pageable pageable) {

        Specification<Ticket> ticketSpecification = new GenericSpecification<>(filters);
        Page<Ticket> tickets = ticketRepository.findAll(ticketSpecification, pageable);

        return tickets.map(ticket -> {
            TicketWithTransactionDto dto = new TicketWithTransactionDto();
            BeanUtils.copyProperties(ticket, dto);

            try {
                Map<String, Object> transactionDetails = getTransactionDetailsMap(ticket.getTransactionId(), ticket.getRaisedBy());
                if (transactionDetails != null) {
                    dto.setTranStatus((String) transactionDetails.get("tran_status"));
                    dto.setBalBeforeTran(toBigDecimal(transactionDetails.get("bal_before_tran")));
                    dto.setBalAfterTran(toBigDecimal(transactionDetails.get("bal_after_tran")));
                    dto.setDateOfTransaction(toLocalDate(transactionDetails.get("date_of_transaction")));
                    dto.setAmount(toBigDecimal(transactionDetails.get("amount")));
                    dto.setService((String) transactionDetails.get("service"));
                    dto.setActionOnBalanceAmount((String) transactionDetails.get("action_on_balance_amount"));
                }
            } catch (Exception e) {
                log.warn("Transaction details not found for transactionId: {}", ticket.getTransactionId());
            }

            return dto;
        });
    }


    public Map<String, Object> getTransactionDetailsMap(String transactionId, String hierarchyId) {
        if (transactionId == null || hierarchyId == null) return null;

        String hierarchyType = commonMethods.extractHierarchyType(hierarchyId);
        String entityName = commonMethods.getEntityName(hierarchyType);

        WalletType[] walletTypes = {WalletType.PREPAID, WalletType.POSTPAID};

        for (WalletType walletType : walletTypes) {
            String tableName = entityName + "_" + walletType.name().toLowerCase() + "_transaction_details";
            String sql = "SELECT * FROM " + tableName + " WHERE transaction_id = :transactionId";

            try {
                Query query = entityManager.createNativeQuery(sql, Map.class);
                query.setParameter("transactionId", transactionId);
                return (Map<String, Object>) query.getSingleResult();
            } catch (NoResultException e) {
                // Try next table
            }
        }
        return null;
    }

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return null;
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        try {
            return new BigDecimal(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate toLocalDate(Object obj) {
        if (obj == null) return null;
        if (obj instanceof LocalDate) return (LocalDate) obj;
        try {
            return LocalDate.parse(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String toStringValue(Object obj) {
        return obj != null ? obj.toString() : null;
    }

    @Override
    public Ticket getTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found with ID: " + id));
    }

    @Override
    public Ticket updateStatusAndRemark(Long id, TicketStatusUpdateDto dto) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found with ID: " + id));

        if (dto.getStatus() != null) {
            ticket.setStatus(dto.getStatus());
        }

        if (dto.getRemark() != null) {
            ticket.setRemark(dto.getRemark());
        }

        ticket.setUpdatedAt(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }

    @Override
    public void deleteTicketById(Long id) {
        if (!ticketRepository.existsById(id)) {
            throw new RuntimeException("Ticket not found with ID: " + id);
        }
        ticketRepository.deleteById(id);
    }


}

