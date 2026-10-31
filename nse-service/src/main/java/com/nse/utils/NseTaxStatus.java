package com.nse.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * Single source of truth for everything the NSE APIs derive from a client's tax status code.
 *
 * <p>NSEMF_API_Details_V1.9.7 keys a dozen fields off the tax status: gender (UCC field 6),
 * the guardian block (18/21/121), the foreign address block (82-92), {@code indian_mobile_no}
 * (93), {@code nomination_opt} (122), and on the FATCA_COMMON side {@code exemp_code},
 * {@code sdf_flag}, {@code ubo_appl} and {@code ubo_df}. The spec states the rule in terms of a
 * category rather than a code list - see FATCA_COMMON / UBO_APPL:
 *
 * <blockquote>IF Tax Status Category="Non-Individual" OR Tax Status='HUF' Then It can be 'Y' or
 * 'N' Else It should be 'N'</blockquote>
 *
 * <p>Codes are the ones in {@link NseUtils#clientTaxStatusCodeOrName()}. A code that is not in
 * the table below is treated as {@link Category#NON_INDIVIDUAL} and logged, matching the spec's
 * "default 'Y' for Entities" stance; it will be rejected by NSE anyway because tax_status is a
 * mandatory enumerated field.
 */
public final class NseTaxStatus {

    private static final Logger logger = LoggerFactory.getLogger(NseTaxStatus.class);

    private NseTaxStatus() {
    }

    public enum Category {
        INDIVIDUAL,
        HUF,
        NON_INDIVIDUAL
    }

    public enum Residency {
        RESIDENT,
        NRI,
        /** NRI, but exempt from the foreign address block - UCC field 82 "EXCEPT FOR SEAFARER". */
        SEAFARER
    }

    private static final Set<String> INDIVIDUAL = Set.of(
            "01", // INDIVIDUAL
            "02", // ON BEHALF OF MINOR
            "11", // NRI-OTHERS
            "21", // NRI - REPATRIABLE (NRE)
            "24", // NRI - REPATRIABLE (NRO)
            "26", // NRI CHILD
            "28", // NRI - MINOR (NRO)
            "41", // QFI - INDIVIDUAL
            "42", // QFI - MINORS
            "61", // OCI - REPATRIATION
            "62", // OCI - NON REPATRIATION
            "70", // PERSON OF INDIAN ORIGIN
            "77", // SEAFARER NRE
            "78"  // SEAFARER NRO
    );

    private static final Set<String> HUF = Set.of(
            "03", // HUF
            "27", // NRI - HUF (NRO)
            "29"  // NRI - HUF (NRE)
    );

    /**
     * Listed out rather than inferred as "everything else" so that a code NSE adds later shows up
     * as unknown in the log instead of silently taking the entity path.
     */
    private static final Set<String> NON_INDIVIDUAL = Set.of(
            "04", // COMPANY
            "05", // AOP
            "06", // PARTNERSHIP FIRM
            "07", // BODY CORPORATE
            "08", // TRUST
            "09", // SOCIETY
            "10", // OTHERS
            "12", // DFI
            "13", // SOLE PROPRIETORSHIP
            "22", // OCB
            "23", // FII
            "25", // OVERSEAS CORP. BODY - OTHERS
            "31", // PROVIDEND FUND
            "32", // SUPER ANNUATION FUND
            "33", // GRATUITY FUND
            "34", // PENSION FUND
            "36", // MUTUAL FUNDS FOF SCHEMES
            "37", // NPS TRUST
            "38", // GLOBAL DEVELOPMENT NETWORK
            "39", // FCRA
            "43", // QFI - CORPORATE
            "44", // QFI - PENSION FUNDS
            "45", // QFI - HEDGE FUNDS
            "46", // QFI - MUTUAL FUNDS
            "47", // LLP
            "48", // NON-PROFIT ORGANIZATION [NPO]
            "51", // PUBLIC LIMITED COMPANY
            "52", // PRIVATE LIMITED COMPANY
            "53", // UNLISTED COMPANY
            "54", // MUTUAL FUNDS
            "55", // FPI - CATEGORY I
            "56", // FPI - CATEGORY II
            "57", // FPI - CATEGORY III
            "58", // FINANCIAL INSTITUTIONS
            "59", // BODY OF INDIVIDUALS
            "60", // INSURANCE COMPANY
            "72", // GOVERNMENT BODY
            "73", // DEFENCE ESTABLISHMENT
            "74", // NON - GOVERNMENT ORGANISATION
            "75", // BANK/ CO-OPERATIVE BANK
            "76", // ARTIFICIAL JURIDICAL PERSON
            "79"  // LOCAL AUTHORITY
    );

    /**
     * Tax statuses that place the client outside India. {@code 22} (OCB) and {@code 25} (Overseas
     * Corp. Body - Others) are overseas *entities* rather than NRIs and are deliberately left out:
     * the spec wording is "MANDATORY FOR NRI" / "WHEN TAX STATUS IS NRI/NRO". Confirm with NSE
     * before adding them.
     */
    private static final Set<String> NRI = Set.of(
            "11", "21", "24", "26", "27", "28", "29", "61", "62", "70", "77", "78"
    );

    private static final Set<String> SEAFARER = Set.of("77", "78");

    private static final Set<String> MINOR = Set.of(
            "02", // ON BEHALF OF MINOR
            "26", // NRI CHILD
            "28", // NRI - MINOR (NRO)
            "42"  // QFI - MINORS
    );

    /** UCC field 121 - only 06, 13 and 23 are accepted by NSE. */
    private static final Map<String, String> GUARDIAN_RELATION = Map.ofEntries(
            Map.entry("FATHER", "06"),
            Map.entry("MOTHER", "13"),
            Map.entry("COURT APPOINTED LEGAL GUARDIAN", "23"),
            Map.entry("LEGAL GUARDIAN", "23"),
            Map.entry("COURT APPOINTED GUARDIAN", "23"),
            Map.entry("GUARDIAN", "23"),
            Map.entry("06", "06"),
            Map.entry("13", "13"),
            Map.entry("23", "23")
    );

    /**
     * Trims and zero-pads so that "2" and "02" resolve to the same status.
     */
    private static String normalize(String taxStatusCode) {
        if (taxStatusCode == null) {
            return "";
        }
        String code = taxStatusCode.trim();
        return code.length() == 1 ? "0" + code : code;
    }

    public static Category category(String taxStatusCode) {
        String code = normalize(taxStatusCode);
        if (INDIVIDUAL.contains(code)) {
            return Category.INDIVIDUAL;
        }
        if (HUF.contains(code)) {
            return Category.HUF;
        }
        if (!NON_INDIVIDUAL.contains(code)) {
            logger.warn("Unknown NSE tax status code '{}' - defaulting to NON_INDIVIDUAL", taxStatusCode);
        }
        return Category.NON_INDIVIDUAL;
    }

    public static Residency residency(String taxStatusCode) {
        String code = normalize(taxStatusCode);
        if (SEAFARER.contains(code)) {
            return Residency.SEAFARER;
        }
        return NRI.contains(code) ? Residency.NRI : Residency.RESIDENT;
    }

    public static boolean isIndividual(String taxStatusCode) {
        return category(taxStatusCode) == Category.INDIVIDUAL;
    }

    /** True for minors and minor-equivalents, where the guardian's PAN stands in for the holder's. */
    public static boolean isMinor(String taxStatusCode) {
        return MINOR.contains(normalize(taxStatusCode));
    }

    /** True for any client outside India, seafarers included. */
    public static boolean isNri(String taxStatusCode) {
        return residency(taxStatusCode) != Residency.RESIDENT;
    }

    /**
     * UCC fields 82-92. True for NRIs except seafarers, who are explicitly exempted by field 82.
     */
    public static boolean requiresForeignAddress(String taxStatusCode) {
        return residency(taxStatusCode) == Residency.NRI;
    }

    /**
     * The spec's "Tax Status Category='Non-Individual' OR Tax Status='HUF'" test. Drives
     * {@code data_src}, {@code nffe_catg}, {@code exemp_code}, {@code sdf_flag}, {@code ubo_appl}
     * and {@code ubo_df} on FATCA_COMMON.
     */
    public static boolean isEntityForFatca(String taxStatusCode) {
        Category category = category(taxStatusCode);
        return category == Category.NON_INDIVIDUAL || category == Category.HUF;
    }

    /**
     * UCC field 122. Nomination is an individual-holder facility, and is not offered on a minor's
     * folio - the guardian already acts for the holder.
     */
    public static boolean nominationAllowed(String taxStatusCode) {
        return isIndividual(taxStatusCode) && !isMinor(taxStatusCode);
    }

    /**
     * Maps a guardian relationship label onto the codes NSE accepts for UCC field 121
     * (06 father / 13 mother / 23 court appointed legal guardian). Anything unrecognised is
     * returned unchanged, following the same pass-through convention as the mobile/email
     * declaration flags, so that an already-coded value survives and a bad one fails loudly at NSE.
     */
    public static String guardianRelationCode(String relationship) {
        if (relationship == null) {
            return "";
        }
        String key = relationship.trim();
        if (key.isEmpty()) {
            return "";
        }
        return GUARDIAN_RELATION.getOrDefault(key.toUpperCase(java.util.Locale.ROOT), key);
    }
}
