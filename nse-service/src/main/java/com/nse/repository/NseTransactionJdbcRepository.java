package com.nse.repository;

import com.nse.model.NseTransactions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Repository
public class NseTransactionJdbcRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public int[] batchUpdateMandateStatus(List<NseTransactions> transactions) {
        String sql = "UPDATE nse_transactions SET " +
                "order_status = ?, " +
                "remarks = ?, " +
                "payment_status = ? " +
                "WHERE client_name = ? AND reg_id = ?";

        int batchSize = 50; // Example: process 50 updates at a time
        List<int[]> results = new ArrayList<>();

        for (int i = 0; i < transactions.size(); i += batchSize) {
            int end = Math.min(i + batchSize, transactions.size());
            List<Object[]> batchArgs = new ArrayList<>();

            for (int j = i; j < end; j++) {
                NseTransactions tx = transactions.get(j);
                Object[] args = new Object[] {
                        tx.getOrder_status(),
                        tx.getRemarks(),
                        tx.getPayment_status(),
                        tx.getClient_name(),
                        tx.getReg_id()
                };
                batchArgs.add(args);
            }

            results.add(jdbcTemplate.batchUpdate(sql, batchArgs));
        }

        // Optionally flatten the result array
        return results.stream()
                .flatMapToInt(Arrays::stream)
                .toArray();
    }
}
