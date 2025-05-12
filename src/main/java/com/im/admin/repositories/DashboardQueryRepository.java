package com.im.admin.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class DashboardQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Map<String, Object> fetchTotalTransactionSums() {
        String sql = "SELECT " +
                "(SELECT COALESCE(SUM(amount), 0) FROM retailer_prepaid_transaction_details) AS retailer_prepaid, " +
                "(SELECT COALESCE(SUM(amount), 0) FROM distributor_prepaid_transaction_details) AS distributor_prepaid, " +
                "(SELECT COALESCE(SUM(amount), 0) FROM partner_prepaid_transaction_details) AS partner_prepaid, " +
                "(SELECT COALESCE(SUM(amount), 0) FROM super_distributor_prepaid_transaction_details) AS super_distributor_prepaid, " +
                "(SELECT COALESCE(SUM(amount), 0) FROM enterprises_prepaid_transaction_details) AS enterprises_prepaid, " +
                "(SELECT COALESCE(SUM(amount), 0) FROM white_label_prepaid_transaction_details) AS white_label_prepaid, " +
                "(SELECT COALESCE(SUM(amount), 0) FROM retailer_postpaid_transaction_details) AS retailer_postpaid, " +
                "(SELECT COALESCE(SUM(amount), 0) FROM distributor_postpaid_transaction_details) AS distributor_postpaid, " +
                "(SELECT COALESCE(SUM(amount), 0) FROM partner_postpaid_transaction_details) AS partner_postpaid, " +
                "(SELECT COALESCE(SUM(amount), 0) FROM super_distributor_postpaid_transaction_details) AS super_distributor_postpaid, " +
                "(SELECT COALESCE(SUM(amount), 0) FROM enterprises_postpaid_transaction_details) AS enterprises_postpaid, " +
                "(SELECT COALESCE(SUM(amount), 0) FROM white_label_postpaid_transaction_details) AS white_label_postpaid, " +
                "(SELECT COALESCE(SUM(amount), 0) FROM retailer_prepaid_transaction_details WHERE tran_status = 'PENDING') AS retailer_prepaid_pending, " +
                "(SELECT COALESCE(SUM(amount), 0) FROM distributor_prepaid_transaction_details WHERE tran_status = 'PENDING') AS distributor_prepaid_pending, " +
                "(SELECT COALESCE(SUM(amount), 0) FROM partner_prepaid_transaction_details WHERE tran_status = 'PENDING') AS partner_prepaid_pending, " +
                "(SELECT COALESCE(SUM(amount), 0) FROM super_distributor_prepaid_transaction_details WHERE tran_status = 'PENDING') AS super_distributor_prepaid_pending, " +
                "(SELECT COALESCE(SUM(amount), 0) FROM enterprises_prepaid_transaction_details WHERE tran_status = 'PENDING') AS enterprises_prepaid_pending, " +
                "(SELECT COALESCE(SUM(amount), 0) FROM white_label_prepaid_transaction_details WHERE tran_status = 'PENDING') AS white_label_prepaid_pending, " +
                "(SELECT COALESCE(SUM(amount), 0) FROM retailer_postpaid_transaction_details WHERE tran_status = 'PENDING') AS retailer_postpaid_pending, " +
                "(SELECT COALESCE(SUM(amount), 0) FROM distributor_postpaid_transaction_details WHERE tran_status = 'PENDING') AS distributor_postpaid_pending, " +
                "(SELECT COALESCE(SUM(amount), 0) FROM partner_postpaid_transaction_details WHERE tran_status = 'PENDING') AS partner_postpaid_pending, " +
                "(SELECT COALESCE(SUM(amount), 0) FROM super_distributor_postpaid_transaction_details WHERE tran_status = 'PENDING') AS super_distributor_postpaid_pending, " +
                "(SELECT COALESCE(SUM(amount), 0) FROM enterprises_postpaid_transaction_details WHERE tran_status = 'PENDING') AS enterprises_postpaid_pending, " +
                "(SELECT COALESCE(SUM(amount), 0) FROM white_label_postpaid_transaction_details WHERE tran_status = 'PENDING') AS white_label_postpaid_pending";

        Object[] result = (Object[]) entityManager.createNativeQuery(sql).getSingleResult();

        Map<String, Object> map = new HashMap<>();
        map.put("retailer_prepaid", result[0]);
        map.put("distributor_prepaid", result[1]);
        map.put("partner_prepaid", result[2]);
        map.put("super_distributor_prepaid", result[3]);
        map.put("enterprise_prepaid", result[4]);
        map.put("whitelabel_prepaid", result[5]);
        map.put("retailer_postpaid", result[6]);
        map.put("distributor_postpaid", result[7]);
        map.put("partner_postpaid", result[8]);
        map.put("super_distributor_postpaid", result[9]);
        map.put("enterprise_postpaid", result[10]);
        map.put("whitelabel_postpaid", result[11]);

        // Add pending sums to the map
        map.put("retailer_prepaid_pending", result[12]);
        map.put("distributor_prepaid_pending", result[13]);
        map.put("partner_prepaid_pending", result[14]);
        map.put("super_distributor_prepaid_pending", result[15]);
        map.put("enterprises_prepaid_pending", result[16]);
        map.put("white_label_prepaid_pending", result[17]);
        map.put("retailer_postpaid_pending", result[18]);
        map.put("distributor_postpaid_pending", result[19]);
        map.put("partner_postpaid_pending", result[20]);
        map.put("super_distributor_postpaid_pending", result[21]);
        map.put("enterprises_postpaid_pending", result[22]);
        map.put("white_label_postpaid_pending", result[23]);

        return map;
    }

}
