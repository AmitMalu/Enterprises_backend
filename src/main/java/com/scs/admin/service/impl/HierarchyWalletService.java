package com.scs.admin.service.impl;

/**
 * @author SHUBHAM KHOPADE
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class HierarchyWalletService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /*
     * IMPORTANT:
     * Verify these table names and hierarchy ID column names
     * against your actual database.
     */
    private static final Map<String, WalletConfiguration>
            PREPAID_TABLE_CONFIGURATION = Map.ofEntries(

            Map.entry(
                    "RT",
                    new WalletConfiguration(
                            "retailer_prepaid_amount",
                            "retailer_id"
                    )
            ),

            Map.entry(
                    "DT",
                    new WalletConfiguration(
                            "distributor_prepaid_amount",
                            "distributor_id"
                    )
            ),

            Map.entry(
                    "PT",
                    new WalletConfiguration(
                            "partner_prepaid_amount",
                            "partner_id"
                    )
            ),

            Map.entry(
                    "SD",
                    new WalletConfiguration(
                            "super_distributor_prepaid_amount",
                            "super_distributor_id"
                    )
            ),

            Map.entry(
                    "WL",
                    new WalletConfiguration(
                            "white_label_prepaid_amount",
                            "white_label_id"
                    )
            ),

            Map.entry(
                    "AD",
                    new WalletConfiguration(
                            "admin_prepaid_amount",
                            "admin_id"
                    )
            )

//            Map.entry(
//                    "AU",
//                    new WalletConfiguration(
//                            "admin_user_prepaid_amount",
//                            "admin_user_id"
//                    )
//            ),
//
//            Map.entry(
//                    "WLAU",
//                    new WalletConfiguration(
//                            "white_label_user_prepaid_amount",
//                            "white_label_user_id"
//                    )
//            )
    );

    private static final Map<String, WalletConfiguration>
            POSTPAID_TABLE_CONFIGURATION = Map.ofEntries(

            Map.entry(
                    "RT",
                    new WalletConfiguration(
                            "retailer_postpaid_amount",
                            "retailer_id"
                    )
            ),

            Map.entry(
                    "DT",
                    new WalletConfiguration(
                            "distributor_postpaid_amount",
                            "distributor_id"
                    )
            ),

            Map.entry(
                    "PT",
                    new WalletConfiguration(
                            "partner_postpaid_amount",
                            "partner_id"
                    )
            ),

            Map.entry(
                    "SD",
                    new WalletConfiguration(
                            "super_distributor_postpaid_amount",
                            "super_distributor_id"
                    )
            ),

            Map.entry(
                    "WL",
                    new WalletConfiguration(
                            "white_label_postpaid_amount",
                            "white_label_id"
                    )
            ),

            Map.entry(
                    "AD",
                    new WalletConfiguration(
                            "admin_postpaid_amount",
                            "admin_id"
                    )
            ),

            Map.entry(
                    "AU",
                    new WalletConfiguration(
                            "admin_user_postpaid_amount",
                            "admin_user_id"
                    )
            ),

            Map.entry(
                    "WLAU",
                    new WalletConfiguration(
                            "white_label_user_postpaid_amount",
                            "white_label_user_id"
                    )
            )
    );

    public Map<String, Object> getWalletInformation(
            String hierarchyCode,
            String hierarchyId
    ) {

        log.info(
                "Fetching wallet information for hierarchyId: {}, hierarchyCode: {}",
                hierarchyId,
                hierarchyCode
        );

        Map<String, Object> walletInformation =
                new LinkedHashMap<>();

        walletInformation.put(
                "prepaid",
                fetchWallet(
                        PREPAID_TABLE_CONFIGURATION.get(
                                hierarchyCode
                        ),
                        hierarchyId
                )
        );

        walletInformation.put(
                "postpaid",
                fetchWallet(
                        POSTPAID_TABLE_CONFIGURATION.get(
                                hierarchyCode
                        ),
                        hierarchyId
                )
        );

        return walletInformation;
    }

    private Map<String, Object> fetchWallet(
            WalletConfiguration configuration,
            String hierarchyId
    ) {

        if (configuration == null) {
            return createUnavailableResponse(
                    "Wallet configuration is not available"
            );
        }

        String sql = """
                SELECT *
                FROM %s
                WHERE %s = :hierarchyId
                LIMIT 1
                """.formatted(
                configuration.tableName(),
                configuration.hierarchyColumn()
        );

        try {

            MapSqlParameterSource parameters =
                    new MapSqlParameterSource()
                            .addValue(
                                    "hierarchyId",
                                    hierarchyId
                            );

            List<Map<String, Object>> rows =
                    jdbcTemplate.queryForList(
                            sql,
                            parameters
                    );

            if (rows.isEmpty()) {
                return createUnavailableResponse(
                        "Wallet record not found"
                );
            }

            Map<String, Object> wallet =
                    new LinkedHashMap<>();

            wallet.put("available", true);
            wallet.putAll(rows.get(0));

            return wallet;

        } catch (Exception exception) {

            log.error(
                    "Unable to fetch wallet from table: {} for hierarchyId: {}",
                    configuration.tableName(),
                    hierarchyId,
                    exception
            );

            /*
             * This prevents the entire profile API from failing
             * when one wallet table is not applicable or its name
             * has not yet been configured correctly.
             */
            return createUnavailableResponse(
                    "Unable to fetch wallet information"
            );
        }
    }

    private Map<String, Object> createUnavailableResponse(
            String message
    ) {

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("available", false);
        response.put("message", message);

        return response;
    }

    private record WalletConfiguration(
            String tableName,
            String hierarchyColumn
    ) {
    }
}
