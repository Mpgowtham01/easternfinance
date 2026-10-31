package com.nse.repository;

import com.nse.model.NseTransactions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class NseTransactionJdbcRepository {

    private static final int BATCH_SIZE = 500;

    /**
     * How many reg_ids go into a single IN (...) lookup. Each chunk is one table scan while the
     * (client_name, reg_id) index is missing, so keep this high - 5000 ids is ~5001 placeholders,
     * far below MySQL's 65535 limit.
     */
    private static final int LOOKUP_CHUNK_SIZE = 5000;

    /** The order_status / remarks currently stored against a reg_id. */
    public record OrderStatusRow(String orderStatus, String remarks) {}

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

    /**
     * Looks up the order_status / remarks currently stored for the given reg_ids, in chunked
     * IN (...) queries. Lets the caller skip orders that are not in the table at all, or whose
     * status has not moved, so we never send an UPDATE that cannot change a row.
     *
     * Without an index on (client_name, reg_id) this costs one table scan per chunk, which is
     * still vastly cheaper than one scan per order.
     *
     * @return reg_id -> stored values, containing only reg_ids that exist for this client
     */
    public Map<String, OrderStatusRow> findExistingOrderStatuses(String clientName, List<String> regIds) {
        Map<String, OrderStatusRow> existing = new HashMap<>();
        if (clientName == null || regIds == null || regIds.isEmpty()) {
            return existing;
        }

        int totalChunks = (regIds.size() + LOOKUP_CHUNK_SIZE - 1) / LOOKUP_CHUNK_SIZE;

        for (int i = 0; i < regIds.size(); i += LOOKUP_CHUNK_SIZE) {
            int end = Math.min(i + LOOKUP_CHUNK_SIZE, regIds.size());
            List<String> chunk = regIds.subList(i, end);

            StringBuilder sql = new StringBuilder(
                    "SELECT reg_id, order_status, remarks FROM nse_transactions WHERE client_name = ? AND reg_id IN (");
            for (int j = 0; j < chunk.size(); j++) {
                sql.append(j == 0 ? "?" : ",?");
            }
            sql.append(")");

            Object[] args = new Object[chunk.size() + 1];
            args[0] = clientName;
            for (int j = 0; j < chunk.size(); j++) {
                args[j + 1] = chunk.get(j);
            }

            long chunkStart = System.currentTimeMillis();
            jdbcTemplate.query(sql.toString(), args, rs -> {
                existing.put(rs.getString("reg_id"),
                        new OrderStatusRow(rs.getString("order_status"), rs.getString("remarks")));
            });

            System.out.println("   ↳ lookup " + ((i / LOOKUP_CHUNK_SIZE) + 1) + "/" + totalChunks
                    + " | ids " + (i + 1) + "-" + end
                    + " | found " + existing.size() + " so far"
                    + " | " + (System.currentTimeMillis() - chunkStart) + " ms");
        }

        return existing;
    }

    /**
     * Updates order_status / remarks for many orders using a single multi-row statement per chunk:
     *
     *   UPDATE nse_transactions
     *      SET order_status = CASE reg_id WHEN ? THEN ? ... END,
     *          remarks      = CASE reg_id WHEN ? THEN ? ... END
     *    WHERE client_name = ? AND reg_id IN (?, ?, ...)
     *
     * A plain JDBC batch still makes MySQL evaluate the WHERE clause once per statement, so with
     * no index on (client_name, reg_id) it costs one full table scan per order - 241 orders took
     * over three minutes. Folding them into one statement costs one scan per chunk instead.
     *
     * Add the covering index and this becomes an index range scan, which is what you actually want:
     *   CREATE INDEX idx_nse_tx_client_reg ON nse_transactions (client_name, reg_id);
     *
     * @return total number of rows the database reported as changed
     */
    @Transactional
    public int batchUpdateOrderStatus(List<NseTransactions> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return 0;
        }

        int totalAffected = 0;
        int totalChunks = (transactions.size() + BATCH_SIZE - 1) / BATCH_SIZE;
        long allStart = System.currentTimeMillis();

        for (int i = 0; i < transactions.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, transactions.size());
            List<NseTransactions> chunk = transactions.subList(i, end);

            StringBuilder statusCase = new StringBuilder();
            StringBuilder remarksCase = new StringBuilder();
            StringBuilder inList = new StringBuilder();

            // Order matters: every status CASE arg, then every remarks CASE arg, then client_name,
            // then the IN list - the same order the placeholders appear in the final SQL.
            List<Object> statusArgs = new ArrayList<>(chunk.size() * 2);
            List<Object> remarksArgs = new ArrayList<>(chunk.size() * 2);
            List<Object> inArgs = new ArrayList<>(chunk.size());

            for (NseTransactions tx : chunk) {
                statusCase.append(" WHEN ? THEN ?");
                statusArgs.add(tx.getReg_id());
                statusArgs.add(tx.getOrder_status());

                remarksCase.append(" WHEN ? THEN ?");
                remarksArgs.add(tx.getReg_id());
                remarksArgs.add(tx.getRemarks());

                inList.append(inList.length() == 0 ? "?" : ",?");
                inArgs.add(tx.getReg_id());
            }

            String sql = "UPDATE nse_transactions SET " +
                    "order_status = CASE reg_id" + statusCase + " ELSE order_status END, " +
                    "remarks = CASE reg_id" + remarksCase + " ELSE remarks END " +
                    "WHERE client_name = ? AND reg_id IN (" + inList + ")";

            List<Object> args = new ArrayList<>(statusArgs.size() + remarksArgs.size() + 1 + inArgs.size());
            args.addAll(statusArgs);
            args.addAll(remarksArgs);
            args.add(chunk.get(0).getClient_name());
            args.addAll(inArgs);

            long chunkStart = System.currentTimeMillis();
            int affected = jdbcTemplate.update(sql, args.toArray());
            totalAffected += affected;

            System.out.println("   ↳ batch " + ((i / BATCH_SIZE) + 1) + "/" + totalChunks
                    + " | rows " + (i + 1) + "-" + end
                    + " | " + affected + " row(s) changed"
                    + " | " + (System.currentTimeMillis() - chunkStart) + " ms");
        }

        System.out.println("   ↳ all " + totalChunks + " batch(es) done in "
                + (System.currentTimeMillis() - allStart) + " ms (commit still pending)");

        return totalAffected;
    }
}
