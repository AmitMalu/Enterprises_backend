package com.im.admin.service.impl;

import com.im.admin.dto.AddTicketCommentDto;
import com.im.admin.dto.RaiseTicketRequestDto;
import com.im.admin.dto.TicketIssueTypeDto;
import com.im.admin.dto.UpdateTicketStatusDto;
import com.im.admin.entities.Ticket;
import com.im.admin.entities.TicketComment;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.repositories.TicketCommentRepository;
import com.im.admin.repositories.TicketRepository;
import com.im.admin.service.TicketService;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.ImageHandlerUtil;
import com.im.admin.util.ImageUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
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

        // Handle exception here
        try {
            String folder = "Ticket";

            String  imagePath = imageUtil.saveImage(screenshotImage, "Ticket");
            ticket.setScreenshotImage(imagePath);

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload screenshot image", e);
        }

        Ticket saveTicket = ticketRepository.save(ticket);
        return saveTicket;
    }

    @Override
    public List<TicketIssueTypeDto> getAllIssueTypes() {
        List<Ticket> allIssues = ticketRepository.findAll();
        return allIssues.stream()
                .map(issue -> new TicketIssueTypeDto(issue.getId(), issue.getIssueType()))
                .toList();
    }






}

