package com.nse.services;

import com.nse.client.AmfiServiceClient;
import com.nse.dto.amfi.AmfiSchemeMasterDTO;
import com.nse.model.NseOnlineSchemeMaster;
import com.nse.model.NseOnlineSipStpSwpMaster;
import com.nse.model.NseOnlineStepUpSchemeMaster;
import com.nse.repository.NseOnlineSchemeMasterRepository;
import com.nse.repository.NseOnlineSipStpSwpMasterRepository;
import com.nse.repository.NseOnlineStepUpSchemeMasterRepository;
import com.nse.utils.NseUtils;
import jakarta.persistence.EntityManager;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestHeader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Service
public class NseAmfiOnlineSchemeMaster {
    @Autowired
    AmfiServiceClient amfiServiceClient;

    @Autowired
    NseOnlineSchemeMasterRepository nseOnlineSchemeMasterRepository;

    @Autowired
    NseOnlineSipStpSwpMasterRepository nseOnlineSipStpSwpMasterRepository;

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private NseOnlineStepUpSchemeMasterRepository nseOnlineStepUpSchemeMasterRepository;


    @Transactional
    public List<String> uploadNSEOnlineSchemeMasterPhy(File file, @RequestHeader("Authorization") String token) throws ParseException {
        BufferedReader dis;
        String error_msg = "";

        SimpleDateFormat sdf2 = new SimpleDateFormat("dd-MM-yyyy");
        SimpleDateFormat sdf3 = new SimpleDateFormat("dd-MMM-yyyy");
        Format format = NumberFormat.getNumberInstance(new Locale("en", "in"));

        List<String> error_list = new ArrayList<String>();
        Integer rowNum = 0;

        String filePath = file.getPath();
        String extension = FilenameUtils.getExtension(filePath);
        String file_upload_status = "";

        int availableCores = Runtime.getRuntime().availableProcessors();
        System.out.println("availableCores: " + availableCores);
        ExecutorService executor = Executors.newFixedThreadPool(availableCores);

        List<String> batch = new ArrayList<>();
        int batchSize = 100;

        List<Future<?>> futures = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        try
        {
            String str;
            System.out.println("extension:" + extension);
            if(extension.equalsIgnoreCase("txt"))
            {
                FileReader fr = new FileReader(file);
                dis = new BufferedReader(fr);

                int line_no = 0;
                System.out.println("uploadNSEOnlineSchemeMasterPhy start Date: " + new Date());

                List<AmfiSchemeMasterDTO> schemeMappingList = amfiServiceClient.getActiveWithIsinNo(token);

                entityManager.createNativeQuery("TRUNCATE TABLE nse_online_scheme_master").executeUpdate();

                while ((str = dis.readLine()) != null) {

                    int colLength = str.split("\\|", -1).length;

                    if(colLength != 44) {

                        error_msg = "please check the file. it is not matching with column counts. it is need to be: 44 columns. but it is have: " + colLength;
                        error_list.add(error_msg);
                        return error_list;
                    }

                    if(line_no == 0){
                        line_no++;
                        continue;
                    }
                    batch.add(str);
                    if (batch.size() == batchSize) {
                        List<String> taskBatch = new ArrayList<>(batch);
                        futures.add(executor.submit(() -> processBatch(taskBatch, schemeMappingList)));
                        batch.clear();
                    }
                    line_no++;
                }
                if (!batch.isEmpty()) {
                    List<String> taskBatch = new ArrayList<>(batch);
                    futures.add(executor.submit(() -> processBatch(taskBatch, schemeMappingList)));
                }

                executor.shutdown();
                try {
                    if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException ex) {
                    executor.shutdownNow();
                }

                for (Future<?> future : futures) {
                    try {
                        future.get();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("uploadNSEOnlineSchemeMasterPhy end Date: " + new Date());
                error_msg = "";
                error_msg = "The File has been Uploaded Successfully";
                error_list.add(error_msg);

                dis.close();
                fr.close();
            }
            else{
                error_msg = "";
                error_msg = "File upload error. Not a Txt file!!";
                error_list.add(error_msg);
            }

        } catch ( IOException ex )
        {
            file_upload_status = "File upload error.";

            error_msg = "";
            error_msg = "File upload error.";
            error_list.add(error_msg);
            return error_list;
        }
        error_list.add("The File has been Uploaded Successfully.");
        return error_list;
    }

    private void processBatch(List<String> taskBatch, List<AmfiSchemeMasterDTO> schemeMappingList)
    {

        String unique_sr_no;
        String scheme_code;
        String rta_scheme_code;
        String amc_scheme_code;
        String isin;
        String amc_code;
        String scheme_type;
        String plan_type;
        String scheme_name;
        String purchase_allowed;
        String purchase_transaction_mode;
        String new_purchase_min_amount;
        String additional_purchase_min_amount;
        String additional_purchase_max_amount;
        String purchase_amount_multiplier;
        String purchase_cutoff_time;
        String redemption_allowed;
        String redemption_transaction_mode;
        String redemption_min_qty;
        String redemption_qty_multiplier;
        String redemption_max_qty;
        String redemption_min_amount;
        String redemption_max_amount;
        String redemption_amount_multiplier;
        String redemption_cutoff_time;
        String rta_agent_code;
        String amc_active_flag;
        String div_reinvest_flag;
        String sip_allowed;
        String stp_enabled;
        String swp_enabled;
        String switch_allowed;
        String settlement_type;
        String amc_ind;
        String face_value;
        String scheme_start_date;
        String maturity_date;
        String exit_load_flag;
        String exit_load;
        String lock_in_period_flag;
        String lock_in_period;
        String channel_partner_code;
        String reopening_date;
        String open_close_ended_scheme;

        String amc_name;
        String scheme_amfi;
        String scheme_category;
        String scheme_amfi_code;
        String scheme_amfi_short_name;

        String str = "";
        for (int i = 0; i < taskBatch.size(); i++) {
            str = taskBatch.get(i);
            unique_sr_no = "";
            scheme_code = "";
            rta_scheme_code = "";
            amc_scheme_code = "";
            isin = "";
            amc_code  = "";
            scheme_type = "";
            plan_type = "";
            scheme_name ="";
            purchase_allowed = "";
            purchase_transaction_mode = "";
            new_purchase_min_amount ="";
            additional_purchase_min_amount = "";
            additional_purchase_max_amount = "";
            purchase_amount_multiplier = "";
            purchase_cutoff_time = "";
            redemption_allowed = "";
            redemption_transaction_mode = "";
            redemption_min_qty = "";
            redemption_qty_multiplier = "";
            redemption_max_qty = "";
            redemption_min_amount = "";
            redemption_max_amount = "";
            redemption_amount_multiplier = "";
            redemption_cutoff_time = "";
            rta_agent_code = "";
            amc_active_flag = "";
            div_reinvest_flag = "";
            sip_allowed = "";
            stp_enabled = "";
            swp_enabled = "";
            switch_allowed = "";
            settlement_type = "";
            amc_ind = "";
            face_value = "";
            scheme_start_date = "";
            maturity_date ="";
            exit_load_flag ="";
            exit_load ="";
            lock_in_period_flag = "";
            lock_in_period = "";
            channel_partner_code = "";
            reopening_date ="";
            open_close_ended_scheme = "";

            amc_name = "";
            scheme_amfi = "";
            scheme_category = "";
            scheme_amfi_code = "";
            scheme_amfi_short_name = "";

            String[] splited = str.split("\\|", -1);

            try {

                if (splited.length == 44) {

                    unique_sr_no = splited[0];
                    scheme_code = splited[1];
                    rta_scheme_code = splited[2];
                    amc_scheme_code = splited[3];
                    isin = splited[4];
                    amc_name = splited[5];
                    scheme_type = splited[6];
                    plan_type = splited[7];
                    scheme_name = splited[8];
                    purchase_allowed = splited[9];
                    purchase_transaction_mode = splited[10];
                    new_purchase_min_amount = splited[11];
                    additional_purchase_min_amount = splited[12];
                    additional_purchase_max_amount = splited[13];
                    purchase_amount_multiplier = splited[14];
                    purchase_cutoff_time = splited[15];
                    redemption_allowed = splited[16];
                    redemption_transaction_mode = splited[17];
                    redemption_min_qty = splited[18];
                    redemption_qty_multiplier = splited[19];
                    redemption_max_qty = splited[20];
                    redemption_min_amount = splited[21];
                    redemption_max_amount = splited[22];
                    redemption_amount_multiplier = splited[23];
                    redemption_cutoff_time = splited[24];
                    rta_agent_code = splited[25];
                    amc_active_flag = splited[26];
                    div_reinvest_flag = splited[27];
                    sip_allowed = splited[28];
                    stp_enabled = splited[29];
                    swp_enabled = splited[30];
                    switch_allowed = splited[31];
                    settlement_type = splited[32];
                    amc_ind = splited[33];
                    face_value = splited[34];
                    scheme_start_date = splited[35];
                    maturity_date = splited[36];
                    exit_load_flag = splited[37];
                    exit_load = splited[38];
                    lock_in_period_flag = splited[39];
                    lock_in_period = splited[40];
                    channel_partner_code = splited[41];
                    reopening_date = splited[42];
                    open_close_ended_scheme = splited[43];

                    if(purchase_transaction_mode.equalsIgnoreCase("D")){
                        continue;
                    }

                    amc_code = NseUtils.getAMCNameByNSECompanyName(amc_name);
                    String isin_number = isin;
                    AmfiSchemeMasterDTO mapping = schemeMappingList.stream()
                            .filter(sm -> (Objects.nonNull(sm.getIsin_no()) && sm.getIsin_no().equalsIgnoreCase(isin_number)) ||
                                    (Objects.nonNull(sm.getIsin_divreinvst_no()) && sm.getIsin_divreinvst_no().equalsIgnoreCase(isin_number)))
                            .findAny()
                            .orElse(null);

                    if(mapping != null)
                    {
                        scheme_amfi = mapping.getScheme_amfi();
                        scheme_category = mapping.getScheme_advisorkhoj_category();
                        scheme_amfi_code = mapping.getScheme_amfi_code();
                        scheme_amfi_short_name = mapping.getScheme_amfi_short_name();
                    }
                    else
                    {
                        scheme_amfi = scheme_name;
                        scheme_category = "";
                        scheme_amfi_code = "";
                        scheme_amfi_short_name  = "";
                    }

                    NseOnlineSchemeMaster nseOnlineSchemeMaster = new NseOnlineSchemeMaster();
                    nseOnlineSchemeMaster.setUniqueSrNo(trimString(unique_sr_no));
                    nseOnlineSchemeMaster.setSchemeCode(trimString(scheme_code));
                    nseOnlineSchemeMaster.setRtaSchemeCode(trimString(rta_scheme_code));
                    nseOnlineSchemeMaster.setAmcSchemeCode(trimString(amc_scheme_code));
                    nseOnlineSchemeMaster.setIsin(isin);
                    nseOnlineSchemeMaster.setAmcCode(amc_code);
                    nseOnlineSchemeMaster.setSchemeType(trimString(scheme_type));
                    nseOnlineSchemeMaster.setPlanType(trimString(plan_type));
                    nseOnlineSchemeMaster.setSchemeName(trimString(scheme_amfi));
                    nseOnlineSchemeMaster.setPurchaseAllowed(trimString(purchase_allowed));
                    nseOnlineSchemeMaster.setPurchaseTransactionMode(trimString(purchase_transaction_mode));

                    nseOnlineSchemeMaster.setNewPurchaseMinAmount(Double.parseDouble(trimString(new_purchase_min_amount)));
                    nseOnlineSchemeMaster.setAdditionalPurchaseMinAmount(Double.parseDouble(trimString(additional_purchase_min_amount)));
                    nseOnlineSchemeMaster.setAdditionalPurchaseMaxAmount(Double.parseDouble(trimString(additional_purchase_max_amount)));
                    nseOnlineSchemeMaster.setPurchaseAmountMultiplier(Double.parseDouble(trimString(purchase_amount_multiplier)));

                    nseOnlineSchemeMaster.setPurchaseCutoffTime(trimString(purchase_cutoff_time));
                    nseOnlineSchemeMaster.setRedemptionAllowed(trimString(redemption_allowed));
                    nseOnlineSchemeMaster.setRedemptionTransactionMode(trimString(redemption_transaction_mode));

                    nseOnlineSchemeMaster.setRedemptionMinQty(Double.parseDouble(trimString(redemption_min_qty)));
                    nseOnlineSchemeMaster.setRedemptionQtyMultiplier(Double.parseDouble(trimString(redemption_qty_multiplier)));
                    nseOnlineSchemeMaster.setRedemptionMaxQty(Double.parseDouble(trimString(redemption_max_qty)));
                    nseOnlineSchemeMaster.setRedemptionMinAmount(Double.parseDouble(trimString(redemption_min_amount)));
                    nseOnlineSchemeMaster.setRedemptionMaxAmount(Double.parseDouble(trimString(redemption_max_amount)));
                    nseOnlineSchemeMaster.setRedemptionAmountMultiplier(Double.parseDouble(trimString(redemption_amount_multiplier)));

                    nseOnlineSchemeMaster.setRedemptionCutoffTime(trimString(redemption_cutoff_time));
                    nseOnlineSchemeMaster.setRtaAgentCode(trimString(rta_agent_code));
                    nseOnlineSchemeMaster.setAmcActiveFlag(trimString(amc_active_flag));
                    nseOnlineSchemeMaster.setDivReinvestFlag(trimString(div_reinvest_flag));
                    nseOnlineSchemeMaster.setSipAllowed(trimString(sip_allowed));
                    nseOnlineSchemeMaster.setStpEnabled(trimString(stp_enabled));
                    nseOnlineSchemeMaster.setSwpEnabled(trimString(swp_enabled));
                    nseOnlineSchemeMaster.setSwitchAllowed(trimString(switch_allowed));
                    nseOnlineSchemeMaster.setSettlementType(trimString(settlement_type));
                    nseOnlineSchemeMaster.setAmcInd(trimString(amc_ind));
                    nseOnlineSchemeMaster.setFaceValue(trimString(face_value));
                    nseOnlineSchemeMaster.setSchemeStartDate(trimString(scheme_start_date));
                    nseOnlineSchemeMaster.setMaturityDate(trimString(maturity_date));
                    nseOnlineSchemeMaster.setExitLoadFlag(trimString(exit_load_flag));
                    nseOnlineSchemeMaster.setExitLoad(trimString(exit_load));
                    nseOnlineSchemeMaster.setLockInPeriodFlag(trimString(lock_in_period_flag));
                    nseOnlineSchemeMaster.setLockInPeriod(trimString(lock_in_period));
                    nseOnlineSchemeMaster.setChannelPartnerCode(trimString(channel_partner_code));
                    nseOnlineSchemeMaster.setReopeningDate(trimString(reopening_date));
                    nseOnlineSchemeMaster.setOpenCloseEndedScheme(trimString(open_close_ended_scheme));
                    nseOnlineSchemeMaster.setCreatedDate(new Date());

                    nseOnlineSchemeMaster.setScheme(trimString(scheme_name));
                    nseOnlineSchemeMaster.setAmcName(amc_name);
                    nseOnlineSchemeMaster.setSchemeCategory(scheme_category);
                    nseOnlineSchemeMaster.setSchemeAmfiCode(scheme_amfi_code);
                    nseOnlineSchemeMaster.setSchemeAmfiShortName(scheme_amfi_short_name);

                    nseOnlineSchemeMasterRepository.save(nseOnlineSchemeMaster);

                }else {
                    System.out.println("splited: " + Arrays.toString(splited));
                }

            }catch (Exception e)
            {
                throw e;
            }
        }
    }
    private String trimString(String str) {
        return org.apache.commons.lang3.StringUtils.trim(str);
    }

    public List<String> uploadNSEOnlineSchemeMasterSip(File file,@RequestHeader("Authorization") String token)
    {
        BufferedReader dis;
        String str;

        String error_msg = "";
        int start_index = 0;
        int end_index = 0;

        SimpleDateFormat sdf2 = new SimpleDateFormat("dd-MM-yyyy");
        SimpleDateFormat sdf3 = new SimpleDateFormat("dd-MMM-yyyy");
        Format format = NumberFormat.getNumberInstance(new Locale("en", "in"));

        List<String> error_list = new ArrayList<String>();
        Integer rowNum = 0;

        String filePath = file.getPath();
        String extension = FilenameUtils.getExtension(filePath);

        String file_upload_status = "";

        int availableCores = Runtime.getRuntime().availableProcessors();
        System.out.println("availableCores: " + availableCores);
        ExecutorService executor = Executors.newFixedThreadPool(availableCores);

        List<String> batch = new ArrayList<>();
        int batchSize = 100;

        List<Future<?>> futures = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

        try
        {
            System.out.println("extension: " + extension);
            if(extension.equalsIgnoreCase("txt"))
            {
                FileReader fr = new FileReader(file);
                dis = new BufferedReader(fr);

                int line_no = 0;

                List<AmfiSchemeMasterDTO> schemeMappingList = amfiServiceClient.getActiveWithIsinNo(token);

                nseOnlineSipStpSwpMasterRepository.deleteByMasterOption("SIP");

                System.out.println("uploadNSEOnlineSchemeMasterSip start Date: " + new Date());
                while ((str = dis.readLine()) != null)
                {

                    int colLength = str.split("\\|", -1).length;

                    if(colLength != 27) {

                        error_msg = "Column mismatch detected in the uploaded file. Expected 27 columns, but found only " + colLength + ".";
                        error_list.add(error_msg);
                        return error_list;
                    }

                    if(line_no == 0){
                        line_no++;
                        continue;
                    }
                    batch.add(str);
                    if (batch.size() == batchSize) {
                        List<String> taskBatch = new ArrayList<>(batch);
                        futures.add(executor.submit(() -> processBatchSIP(taskBatch, schemeMappingList)));
                        batch.clear();
                    }
                    line_no++;
                }
                if (!batch.isEmpty()) {
                    List<String> taskBatch = new ArrayList<>(batch);
                    futures.add(executor.submit(() -> processBatchSIP(taskBatch, schemeMappingList)));
                }
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException ex) {
                    executor.shutdownNow();
                }

                for (Future<?> future : futures) {
                    try {
                        future.get();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("uploadNSEOnlineSchemeMasterSip end Date: " + new Date());
                error_msg = "";
                error_msg = "The File has been Uploaded Successfully";
                error_list.add(error_msg);

                dis.close();
                fr.close();
            }
            else{
                error_msg = "";
                error_msg = "File upload error.Not a Txt file!!";
                error_list.add(error_msg);
            }

        } catch ( IOException ex )
        {
            ex.printStackTrace();
            file_upload_status = "File upload error.";

            error_msg = "";
            error_msg = "File upload error.";
            error_list.add(error_msg);
            return error_list;
        }
        file_upload_status = "The File has been Uploaded Successfully.";
        return error_list;
    }
    private void processBatchSIP(List<String> taskBatch, List<AmfiSchemeMasterDTO> schemeMappingList)
    {

        String amc_code;
        String amc_name;
        String scheme_code;
        String scheme_name;
        String sip_transaction_mode;
        String sip_frequency;
        String sip_dates;
        String sip_minimum_gap;
        String sip_maximum_gap;
        String sip_installment_gap;
        String sip_status;
        String sip_minimum_installment_amount;
        String sip_maximum_installment_amount;
        String sip_multiplier_amount;
        String sip_minimum_installment_numbers;
        String sip_maximum_installment_numbers;
        String scheme_isin;
        String scheme_type;
        String pause_flag;
        String pause_minimum_installments;
        String pause_maximum_installments;
        String pause_modification_count;
        String filler_1;
        String filler_2;
        String filler_3;
        String filler_4;
        String filler_5;

        String scheme_amfi;
        String scheme_category;
        String scheme_amfi_code;
        String scheme_amfi_short_name;

        String str = "";
        for (int i = 0; i < taskBatch.size(); i++) {
            str = taskBatch.get(i);
            amc_code = "";
            amc_name = "";
            scheme_code = "";
            scheme_name = "";
            sip_transaction_mode = "";
            sip_frequency = "";
            sip_dates = "";
            sip_minimum_gap = "";
            sip_maximum_gap = "";
            sip_installment_gap = "";
            sip_status = "";
            sip_minimum_installment_amount = "";
            sip_maximum_installment_amount = "";
            sip_multiplier_amount = "";
            sip_minimum_installment_numbers = "";
            sip_maximum_installment_numbers = "";
            scheme_isin = "";
            scheme_type = "";
            pause_flag = "";
            pause_minimum_installments = "";
            pause_maximum_installments = "";
            pause_modification_count = "";
            filler_1 = "";
            filler_2 = "";
            filler_3 = "";
            filler_4 = "";
            filler_5 = "";

            scheme_amfi = "";
            scheme_category = "";
            scheme_amfi_code = "";
            scheme_amfi_short_name = "";

            amc_name = "";
            scheme_amfi = "";
            scheme_category = "";
            scheme_amfi_code = "";
            scheme_amfi_short_name = "";

            String[] splited = str.split("\\|", -1);

            try {
                if (splited.length == 27) {

                    amc_code = splited[0];
                    amc_name = splited[1];
                    scheme_code = splited[2];
                    scheme_name = splited[3];
                    sip_transaction_mode = splited[4];
                    sip_frequency = splited[5];
                    sip_dates = splited[6];
                    sip_minimum_gap = splited[7];
                    sip_maximum_gap = splited[8];
                    sip_installment_gap = splited[9];
                    sip_status = splited[10];
                    sip_minimum_installment_amount = splited[11];
                    sip_maximum_installment_amount = splited[12];
                    sip_multiplier_amount = splited[13];
                    sip_minimum_installment_numbers = splited[14];
                    sip_maximum_installment_numbers = splited[15];
                    scheme_isin = splited[16];
                    scheme_type = splited[17];
                    pause_flag = splited[18];
                    pause_minimum_installments = splited[19];
                    pause_maximum_installments = splited[20];
                    pause_modification_count = splited[21];
                    filler_1 = splited[22];
                    filler_2 = splited[23];
                    filler_3 = splited[24];
                    filler_4 = splited[25];
                    filler_5 = splited[26];

                    if(sip_transaction_mode.equalsIgnoreCase("D")){
                        continue;
                    }

                    String isin_number = scheme_isin;
                    AmfiSchemeMasterDTO mapping = schemeMappingList.stream()
                            .filter(sm -> (Objects.nonNull(sm.getIsin_no()) && sm.getIsin_no().equalsIgnoreCase(isin_number)) ||
                                    (Objects.nonNull(sm.getIsin_divreinvst_no()) && sm.getIsin_divreinvst_no().equalsIgnoreCase(isin_number)))
                            .findAny()
                            .orElse(null);

                    if(mapping != null)
                    {
                        scheme_amfi = mapping.getScheme_amfi();
                        scheme_category = mapping.getScheme_advisorkhoj_category();
                        scheme_amfi_code = mapping.getScheme_amfi_code();
                        scheme_amfi_short_name = mapping.getScheme_amfi_short_name();
                    }
                    else
                    {
                        scheme_amfi = scheme_name;
                        scheme_category = "";
                        scheme_amfi_code = "";
                        scheme_amfi_short_name  = "";
                    }

                    amc_name = NseUtils.getAMCNameByNSECompanyName(amc_code);
                    NseOnlineSipStpSwpMaster nseOnlineSipStpSwpMaster = new NseOnlineSipStpSwpMaster();

                    nseOnlineSipStpSwpMaster.setAmc_code(amc_code);
                    nseOnlineSipStpSwpMaster.setAmc_name(amc_name);
                    nseOnlineSipStpSwpMaster.setScheme_code(scheme_code);
                    nseOnlineSipStpSwpMaster.setScheme_name(scheme_name);
                    nseOnlineSipStpSwpMaster.setSip_transaction_mode(sip_transaction_mode);
                    nseOnlineSipStpSwpMaster.setSip_frequency(sip_frequency);
                    nseOnlineSipStpSwpMaster.setSip_dates(sip_dates);
                    nseOnlineSipStpSwpMaster.setSip_minimum_gap(sip_minimum_gap);
                    nseOnlineSipStpSwpMaster.setSip_maximum_gap(sip_maximum_gap);
                    nseOnlineSipStpSwpMaster.setSip_installment_gap(sip_installment_gap);
                    nseOnlineSipStpSwpMaster.setSip_status(sip_status);
                    nseOnlineSipStpSwpMaster.setSip_minimum_installment_amount(Double.parseDouble(trimString(sip_minimum_installment_amount)));
                    nseOnlineSipStpSwpMaster.setSip_maximum_installment_amount(Double.parseDouble(trimString(sip_maximum_installment_amount)));
                    nseOnlineSipStpSwpMaster.setSip_multiplier_amount(Double.parseDouble(trimString(sip_multiplier_amount)));
                    nseOnlineSipStpSwpMaster.setSip_minimum_installment_numbers(Double.parseDouble(trimString(sip_minimum_installment_numbers)));
                    nseOnlineSipStpSwpMaster.setSip_maximum_installment_numbers(Double.parseDouble(trimString(sip_maximum_installment_numbers)));
                    nseOnlineSipStpSwpMaster.setScheme_isin(scheme_isin);
                    nseOnlineSipStpSwpMaster.setScheme_type(scheme_type);
                    nseOnlineSipStpSwpMaster.setPause_flag(pause_flag);
                    nseOnlineSipStpSwpMaster.setPause_minimum_installments(pause_minimum_installments);
                    nseOnlineSipStpSwpMaster.setPause_maximum_installments(pause_maximum_installments);
                    if(!pause_modification_count.isEmpty()) {

                        System.out.println("pause_modification_count: " + pause_modification_count);
                    }
                    nseOnlineSipStpSwpMaster.setPause_modification_count(pause_modification_count);
                    nseOnlineSipStpSwpMaster.setFiller_1(filler_1);
                    nseOnlineSipStpSwpMaster.setFiller_2(filler_2);
                    nseOnlineSipStpSwpMaster.setFiller_3(filler_3);
                    nseOnlineSipStpSwpMaster.setFiller_4(filler_4);
                    nseOnlineSipStpSwpMaster.setFiller_5(filler_5);
                    nseOnlineSipStpSwpMaster.setMaster_option("sip");
                    nseOnlineSipStpSwpMaster.setCreated_date(new Date());

                    nseOnlineSipStpSwpMaster.setAmc_name(amc_name);
                    nseOnlineSipStpSwpMaster.setScheme_amfi(scheme_amfi);
                    nseOnlineSipStpSwpMaster.setScheme_category(scheme_category);
                    nseOnlineSipStpSwpMaster.setScheme_amfi_code(scheme_amfi_code);

                    nseOnlineSipStpSwpMasterRepository.save(nseOnlineSipStpSwpMaster);
                }else {

                    System.out.println("splited: " + Arrays.toString(splited));
                }

            }catch (Exception e)
            {
                throw e;
            }
        }


    }

    public List<String> uploadNSEOnlineSchemeMasterStp(File file,@RequestHeader("Authorization") String token)
    {
        BufferedReader dis;
        String str;

        String error_msg = "";
        int start_index = 0;
        int end_index = 0;

        SimpleDateFormat sdf2 = new SimpleDateFormat("dd-MM-yyyy");
        SimpleDateFormat sdf3 = new SimpleDateFormat("dd-MMM-yyyy");
        Format format = NumberFormat.getNumberInstance(new Locale("en", "in"));

        List<String> error_list = new ArrayList<String>();
        Integer rowNum = 0;

        String filePath = file.getPath();
        String extension = FilenameUtils.getExtension(filePath);

        String file_upload_status = "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

        int availableCores = Runtime.getRuntime().availableProcessors();
        System.out.println("availableCores: " + availableCores);
        ExecutorService executor = Executors.newFixedThreadPool(availableCores);

        List<String> batch = new ArrayList<>();
        int batchSize = 100;

        List<Future<?>> futures = new ArrayList<>();

        try
        {
            if(extension.equalsIgnoreCase("txt"))
            {
                FileReader fr = new FileReader(file);
                dis = new BufferedReader(fr);

                int line_no = 0;

                List<AmfiSchemeMasterDTO> schemeMappingList = amfiServiceClient.getActiveWithIsinNo(token);

                nseOnlineSipStpSwpMasterRepository.deleteByMasterOption("STP");

                System.out.println("uploadNSEOnlineSchemeMasterStp start Date: " + new Date());
                while ((str = dis.readLine()) != null)
                {
                    if(line_no == 0){
                        line_no++;
                        continue;
                    }
                    line_no++;
                    int colLength = str.split("\\|", -1).length;
                    if(colLength != 26) {

                        error_msg = "Column mismatch detected in the uploaded file. Expected 26 columns, but found only " + colLength + ".";
                        error_list.add(error_msg);
                        return error_list;
                    }
                    batch.add(str);
                    if (batch.size() == batchSize) {
                        List<String> taskBatch = new ArrayList<>(batch);
                        futures.add(executor.submit(() -> processBatchSTP(taskBatch, schemeMappingList)));
                        batch.clear();
                    }
                }

                if (!batch.isEmpty()) {
                    List<String> taskBatch = new ArrayList<>(batch);
                    futures.add(executor.submit(() -> processBatchSTP(taskBatch, schemeMappingList)));
                }
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException ex) {
                    executor.shutdownNow();
                }

                for (Future<?> future : futures) {
                    try {
                        future.get();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("uploadNSEOnlineSchemeMasterStp end Date: " + new Date());

                error_msg = "";
                error_msg = "The File has been Uploaded Successfully";
                error_list.add(error_msg);

                dis.close();
                fr.close();
            }
            else{
                error_msg = "";
                error_msg = "File upload error.Not a Txt file!!";
                error_list.add(error_msg);
            }

        } catch ( IOException ex )
        {
            ex.printStackTrace();
            file_upload_status = "File upload error.";

            error_msg = "";
            error_msg = "File upload error.";
            error_list.add(error_msg);
            return error_list;
        }
        file_upload_status = "The File has been Uploaded Successfully.";
        return error_list;
    }

    private void processBatchSTP(List<String> taskBatch, List<AmfiSchemeMasterDTO> schemeMappingList)
    {
        String amc_code;
        String amc_name;
        String scheme_code;
        String scheme_name;
        String scheme_isin;
        String scheme_type;
        String astp_transaction_mode;
        String astp_in_minimum_installment_amount;
        String astp_in_maximum_installment_amount;
        String astp_in_multiplier_amount;
        String astp_out_minimum_installment_amount;
        String astp_out_maximum_installment_amount;
        String astp_out_multiplier_amount;
        String astp_minimum_installment_units;
        String astp_maximum_installment_units;
        String astp_multiplier_units;
        String astp_minimum_installment_numbers;
        String astp_maximum_installment_numbers;
        String astp_reg_in;
        String astp_reg_out;
        String astp_frequency;
        String astp_dates;
        String astp_minimum_gap;
        String astp_maximum_gap;
        String astp_installment_gap;
        String astp_status;

        String scheme_amfi;
        String scheme_category;
        String scheme_amfi_code;
        String scheme_amfi_short_name;

        String str = "";
        for (int i = 0; i < taskBatch.size(); i++) {
            str = taskBatch.get(i);

            amc_code = "";
            amc_name = "";
            scheme_code ="";
            scheme_name = "";
            scheme_isin = "";
            scheme_type = "";
            astp_transaction_mode = "";
            astp_in_minimum_installment_amount = "";
            astp_in_maximum_installment_amount = "";
            astp_in_multiplier_amount = "";
            astp_out_minimum_installment_amount = "";
            astp_out_maximum_installment_amount = "";
            astp_out_multiplier_amount = "";
            astp_minimum_installment_units = "";
            astp_maximum_installment_units = "";
            astp_multiplier_units = "";
            astp_minimum_installment_numbers = "";
            astp_maximum_installment_numbers ="";
            astp_reg_in = "";
            astp_reg_out ="";
            astp_frequency = "";
            astp_dates ="";
            astp_minimum_gap ="";
            astp_maximum_gap ="";
            astp_installment_gap = "";
            astp_status = "";

            amc_name = "";
            scheme_amfi = "";
            scheme_category = "";
            scheme_amfi_code = "";
            scheme_amfi_short_name = "";

            String[] splited = str.split("\\|", -1);

            try {
                if (splited.length == 26) {

                    amc_code = splited[0];
                    amc_name = splited[1];
                    scheme_code = splited[2];
                    scheme_name = splited[3];
                    scheme_isin = splited[4];
                    scheme_type = splited[5];
                    astp_transaction_mode = splited[6];
                    astp_in_minimum_installment_amount = splited[7];
                    astp_in_maximum_installment_amount = splited[8];
                    astp_in_multiplier_amount = splited[9];
                    astp_out_minimum_installment_amount = splited[10];
                    astp_out_maximum_installment_amount = splited[11];
                    astp_out_multiplier_amount = splited[12];
                    astp_minimum_installment_units = splited[13];
                    astp_maximum_installment_units = splited[14];
                    astp_multiplier_units = splited[15];
                    astp_minimum_installment_numbers = splited[16];
                    astp_maximum_installment_numbers = splited[17];
                    astp_reg_in = splited[18];
                    astp_reg_out = splited[19];
                    astp_frequency = splited[20];
                    astp_dates = splited[21];
                    astp_minimum_gap = splited[22];
                    astp_maximum_gap = splited[23];
                    astp_installment_gap = splited[24];
                    astp_status = splited[25];

                    if(astp_transaction_mode.equalsIgnoreCase("D")){
                        continue;
                    }
                    String isin_number = scheme_isin;
                    AmfiSchemeMasterDTO mapping = schemeMappingList.stream()
                            .filter(sm -> (Objects.nonNull(sm.getIsin_no()) && sm.getIsin_no().equalsIgnoreCase(isin_number)) ||
                                    (Objects.nonNull(sm.getIsin_divreinvst_no()) && sm.getIsin_divreinvst_no().equalsIgnoreCase(isin_number)))
                            .findAny()
                            .orElse(null);

                    if(mapping != null)
                    {
                        scheme_amfi = mapping.getScheme_amfi();
                        scheme_category = mapping.getScheme_advisorkhoj_category();
                        scheme_amfi_code = mapping.getScheme_amfi_code();
                        scheme_amfi_short_name = mapping.getScheme_amfi_short_name();
                    }
                    else
                    {
                        scheme_amfi = scheme_name;
                        scheme_category = "";
                        scheme_amfi_code = "";
                        scheme_amfi_short_name  = "";
                    }

                    amc_name = NseUtils.getAMCNameByNSECompanyName(amc_code);

                    NseOnlineSipStpSwpMaster nseOnlineSipStpSwpMaster = new NseOnlineSipStpSwpMaster();
                    nseOnlineSipStpSwpMaster.setAmc_code(amc_code);
                    nseOnlineSipStpSwpMaster.setAmc_name(amc_name);
                    nseOnlineSipStpSwpMaster.setScheme_code(scheme_code);
                    nseOnlineSipStpSwpMaster.setScheme_name(scheme_name);
                    nseOnlineSipStpSwpMaster.setScheme_isin(scheme_isin);
                    nseOnlineSipStpSwpMaster.setScheme_type(scheme_type);
                    nseOnlineSipStpSwpMaster.setAstp_transaction_mode(astp_transaction_mode);
                    nseOnlineSipStpSwpMaster.setAstp_in_minimum_installment_amount(Double.parseDouble(trimString(astp_in_minimum_installment_amount)));
                    nseOnlineSipStpSwpMaster.setAstp_in_maximum_installment_amount(Double.parseDouble(trimString(astp_in_maximum_installment_amount)));
                    nseOnlineSipStpSwpMaster.setAstp_in_multiplier_amount(Double.parseDouble(trimString(astp_in_multiplier_amount)));
                    nseOnlineSipStpSwpMaster.setAstp_out_minimum_installment_amount(Double.parseDouble(trimString(astp_out_minimum_installment_amount)));
                    nseOnlineSipStpSwpMaster.setAstp_out_maximum_installment_amount(Double.parseDouble(trimString(astp_out_maximum_installment_amount)));
                    nseOnlineSipStpSwpMaster.setAstp_out_multiplier_amount(Double.parseDouble(trimString(astp_out_multiplier_amount)));
                    nseOnlineSipStpSwpMaster.setAstp_minimum_installment_units(astp_minimum_installment_units);
                    nseOnlineSipStpSwpMaster.setAstp_maximum_installment_units(astp_maximum_installment_units);
                    nseOnlineSipStpSwpMaster.setAstp_multiplier_units(astp_multiplier_units);
                    nseOnlineSipStpSwpMaster.setAstp_minimum_installment_numbers(astp_minimum_installment_numbers);
                    nseOnlineSipStpSwpMaster.setAstp_maximum_installment_numbers(astp_maximum_installment_numbers);
                    nseOnlineSipStpSwpMaster.setAstp_reg_in(astp_reg_in);
                    nseOnlineSipStpSwpMaster.setAstp_reg_out(astp_reg_out);
                    nseOnlineSipStpSwpMaster.setAstp_frequency(astp_frequency);
                    nseOnlineSipStpSwpMaster.setAstp_dates(astp_dates);
                    nseOnlineSipStpSwpMaster.setAstp_minimum_gap(astp_minimum_gap);
                    nseOnlineSipStpSwpMaster.setAstp_maximum_gap(astp_maximum_gap);
                    nseOnlineSipStpSwpMaster.setAstp_installment_gap(astp_installment_gap);
                    nseOnlineSipStpSwpMaster.setAstp_status(astp_status);

                    nseOnlineSipStpSwpMaster.setAmc_name(amc_name);
                    nseOnlineSipStpSwpMaster.setScheme_amfi(scheme_amfi);
                    nseOnlineSipStpSwpMaster.setScheme_category(scheme_category);
                    nseOnlineSipStpSwpMaster.setScheme_amfi_code(scheme_amfi_code);

                    nseOnlineSipStpSwpMaster.setCreated_date(new Date());
                    nseOnlineSipStpSwpMaster.setMaster_option("stp");

                    nseOnlineSipStpSwpMasterRepository.save(nseOnlineSipStpSwpMaster);
                }else {
                    System.out.println("splited: " + Arrays.toString(splited));
                }

            }catch (Exception e)
            {
                throw e;
            }
        }
    }

    public List<String> uploadNSEOnlineSchemeMasterSwp(File file,@RequestHeader("Authorization") String token)
    {
        BufferedReader dis;
        String str;

        String error_msg = "";
        int start_index = 0;
        int end_index = 0;

        SimpleDateFormat sdf2 = new SimpleDateFormat("dd-MM-yyyy");
        SimpleDateFormat sdf3 = new SimpleDateFormat("dd-MMM-yyyy");
        Format format = NumberFormat.getNumberInstance(new Locale("en", "in"));

        List<String> error_list = new ArrayList<String>();
        Integer rowNum = 0;

        String filePath = file.getPath();
        String extension = FilenameUtils.getExtension(filePath);

        String file_upload_status = "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

        int availableCores = Runtime.getRuntime().availableProcessors();
        System.out.println("availableCores: " + availableCores);
        ExecutorService executor = Executors.newFixedThreadPool(availableCores);

        List<String> batch = new ArrayList<>();
        int batchSize = 100;

        List<Future<?>> futures = new ArrayList<>();
        try
        {
            if(extension.equalsIgnoreCase("txt"))
            {
                FileReader fr = new FileReader(file);
                dis = new BufferedReader(fr);

                int line_no = 0;

                List<AmfiSchemeMasterDTO> schemeMappingList = amfiServiceClient.getActiveWithIsinNo(token);

                nseOnlineSipStpSwpMasterRepository.deleteByMasterOption("SWP");

                System.out.println("uploadNSEOnlineSchemeMasterSwp start Date: " + new Date());
                while ((str = dis.readLine()) != null)
                {
                    if(line_no == 0){
                        line_no++;
                        continue;
                    }

                    line_no++;
                    int colLength = str.split("\\|", -1).length;
                    if(colLength != 21) {

                        error_msg = "Column mismatch detected in the uploaded file. Expected 21 columns, but found only " + colLength + ".";
                        error_list.add(error_msg);
                        return error_list;
                    }
                    batch.add(str);
                    if (batch.size() == batchSize) {
                        List<String> taskBatch = new ArrayList<>(batch);
                        futures.add(executor.submit(() -> processBatchSWP(taskBatch, schemeMappingList)));
                        batch.clear();
                    }
                }

                if (!batch.isEmpty()) {
                    List<String> taskBatch = new ArrayList<>(batch);
                    futures.add(executor.submit(() -> processBatchSWP(taskBatch, schemeMappingList)));
                }
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException ex) {
                    executor.shutdownNow();
                }

                for (Future<?> future : futures) {
                    try {
                        future.get();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("uploadNSEOnlineSchemeMasterSwp end Date: " + new Date());

                error_msg = "";
                error_msg = "The File has been Uploaded Successfully";
                error_list.add(error_msg);

                dis.close();
                fr.close();
            }
            else{
                error_msg = "";
                error_msg = "File upload error.Not a Txt file!!";
                error_list.add(error_msg);
            }

        } catch ( IOException ex )
        {
            ex.printStackTrace();
            file_upload_status = "File upload error.";

            error_msg = "";
            error_msg = "File upload error.";
            error_list.add(error_msg);
            return error_list;
        }
        file_upload_status = "The File has been Uploaded Successfully.";
        return error_list;
    }

    private void processBatchSWP(List<String> taskBatch, List<AmfiSchemeMasterDTO> schemeMappingList)
    {
        String amc_code;
        String amc_name;
        String nse_scheme_code;
        String scheme_name;
        String scheme_isin;
        String scheme_type;
        String aswp_transaction_mode;
        String aswp_minimum_installment_amount;
        String aswp_maximum_installment_amount;
        String aswp_multiplier_amount;
        String aswp_minimum_installment_units;
        String aswp_maximum_installment_units;
        String aswp_multiplier_units;
        String aswp_minimum_installment_numbers;
        String aswp_maximum_installment_numbers;
        String aswp_frequency;
        String aswp_dates;
        String aswp_minimum_gap;
        String aswp_maximum_gap;
        String aswp_installment_gap;
        String aswp_status;

        String scheme_amfi;
        String scheme_category;
        String scheme_amfi_code;
        String scheme_amfi_short_name;

        try {
            String str = "";
            for (int i = 0; i < taskBatch.size(); i++) {

                str = taskBatch.get(i);
                amc_code = "";
                amc_name = "";
                nse_scheme_code = "";
                scheme_name = "";
                scheme_isin = "";
                scheme_type = "";
                aswp_transaction_mode = "";
                aswp_minimum_installment_amount = "";
                aswp_maximum_installment_amount = "";
                aswp_multiplier_amount = "";
                aswp_minimum_installment_units = "";
                aswp_maximum_installment_units = "";
                aswp_multiplier_units = "";
                aswp_minimum_installment_numbers = "";
                aswp_maximum_installment_numbers = "";
                aswp_frequency = "";
                aswp_dates = "";
                aswp_minimum_gap = "";
                aswp_maximum_gap = "";
                aswp_installment_gap = "";
                aswp_status = "";

                String[] splited = str.split("\\|", -1);
                if (splited.length == 21) {

                    amc_code = splited[0];
                    amc_name = splited[1];
                    nse_scheme_code = splited[2];
                    scheme_name = splited[3];
                    scheme_isin = splited[4];
                    scheme_type = splited[5];
                    aswp_transaction_mode = splited[6];
                    aswp_minimum_installment_amount = splited[7];
                    aswp_maximum_installment_amount = splited[8];
                    aswp_multiplier_amount = splited[9];
                    aswp_minimum_installment_units = splited[10];
                    aswp_maximum_installment_units = splited[11];
                    aswp_multiplier_units = splited[12];
                    aswp_minimum_installment_numbers = splited[13];
                    aswp_maximum_installment_numbers = splited[14];
                    aswp_frequency = splited[15];
                    aswp_dates = splited[16];
                    aswp_minimum_gap = splited[17];
                    aswp_maximum_gap = splited[18];
                    aswp_installment_gap = splited[19];
                    aswp_status = splited[20];

                    if(aswp_transaction_mode.equalsIgnoreCase("D")){
                        continue;
                    }

                    String isin_number = scheme_isin;
                    AmfiSchemeMasterDTO mapping = schemeMappingList.stream()
                            .filter(sm -> (Objects.nonNull(sm.getIsin_no()) && sm.getIsin_no().equalsIgnoreCase(isin_number)) ||
                                    (Objects.nonNull(sm.getIsin_divreinvst_no()) && sm.getIsin_divreinvst_no().equalsIgnoreCase(isin_number)))
                            .findAny()
                            .orElse(null);

                    if(mapping != null)
                    {
                        scheme_amfi = mapping.getScheme_amfi();
                        scheme_category = mapping.getScheme_advisorkhoj_category();
                        scheme_amfi_code = mapping.getScheme_amfi_code();
                        scheme_amfi_short_name = mapping.getScheme_amfi_short_name();

                    }else
                    {
                        scheme_amfi = scheme_name;
                        scheme_category = "";
                        scheme_amfi_code = "";
                        scheme_amfi_short_name  = "";
                    }

                    amc_name = NseUtils.getAMCNameByNSECompanyName(amc_code);

                    NseOnlineSipStpSwpMaster  nseOnlineSipStpSwpMaster = new NseOnlineSipStpSwpMaster();
                    nseOnlineSipStpSwpMaster.setAmc_code(amc_code);
                    nseOnlineSipStpSwpMaster.setAmc_name(amc_name);
                    nseOnlineSipStpSwpMaster.setScheme_code(nse_scheme_code);
                    nseOnlineSipStpSwpMaster.setScheme_name(scheme_name);
                    nseOnlineSipStpSwpMaster.setScheme_isin(scheme_isin);
                    nseOnlineSipStpSwpMaster.setScheme_type(scheme_type);
                    nseOnlineSipStpSwpMaster.setAswp_transaction_mode(aswp_transaction_mode);
                    nseOnlineSipStpSwpMaster.setAswp_minimum_installment_amount(Double.parseDouble(trimString(aswp_minimum_installment_amount)));
                    nseOnlineSipStpSwpMaster.setAswp_maximum_installment_amount(Double.parseDouble(trimString(aswp_maximum_installment_amount)));
                    nseOnlineSipStpSwpMaster.setAswp_multiplier_amount(Double.parseDouble(trimString(aswp_multiplier_amount)));
                    nseOnlineSipStpSwpMaster.setAswp_minimum_installment_units(aswp_minimum_installment_units);
                    nseOnlineSipStpSwpMaster.setAswp_maximum_installment_units(aswp_maximum_installment_units);
                    nseOnlineSipStpSwpMaster.setAswp_multiplier_units(aswp_multiplier_units);
                    nseOnlineSipStpSwpMaster.setAswp_minimum_installment_numbers(aswp_minimum_installment_numbers);
                    nseOnlineSipStpSwpMaster.setAswp_maximum_installment_numbers(aswp_maximum_installment_numbers);
                    nseOnlineSipStpSwpMaster.setAswp_frequency(aswp_frequency);
                    nseOnlineSipStpSwpMaster.setAswp_dates(aswp_dates);
                    nseOnlineSipStpSwpMaster.setAswp_minimum_gap(aswp_minimum_gap);
                    nseOnlineSipStpSwpMaster.setAswp_maximum_gap(aswp_maximum_gap);
                    nseOnlineSipStpSwpMaster.setAswp_installment_gap(aswp_installment_gap);
                    nseOnlineSipStpSwpMaster.setAswp_status(aswp_status);

                    nseOnlineSipStpSwpMaster.setAmc_name(amc_name);
                    nseOnlineSipStpSwpMaster.setScheme_amfi(scheme_amfi);
                    nseOnlineSipStpSwpMaster.setScheme_category(scheme_category);
                    nseOnlineSipStpSwpMaster.setScheme_amfi_code(scheme_amfi_code);

                    nseOnlineSipStpSwpMaster.setCreated_date(new Date());
                    nseOnlineSipStpSwpMaster.setMaster_option("swp");

                    nseOnlineSipStpSwpMasterRepository.save(nseOnlineSipStpSwpMaster);
                }
            }
        }catch (Exception e)
        {
            throw e;
        }
    }

    public NseOnlineStepUpSchemeMaster isSchemeValidForSipStepUp(String schemeName) {
        return nseOnlineStepUpSchemeMasterRepository.findBySchemeNameAndStepUpFlag(schemeName).orElse(null);
    }
}
