package com.amfi.controller.aum;

import com.amfi.model.AmfiSchemeMaster;
import com.amfi.model.BenchmarkNav;
import com.amfi.model.BenchmarkNavOld;
import com.amfi.model.InvestorPortfolioHealthCheckup;
import com.amfi.repository.BenchmarkNavOldRepository;
import com.amfi.repository.BenchmarkNavRepository;
import com.amfi.response.StatusMessage;
import com.amfi.service.AmfiSchemeMasterService;
import com.amfi.service.InvestorPortfolioHealthCheckupService;
import com.amfi.utils.AmfiUtils;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Hidden
@RestController
@SecurityRequirement(name = "bearerAuth")
public class AmfiFeignClientAumController
{
    @Autowired
    BenchmarkNavOldRepository benchmarkNavOldRepository;

    @Autowired
    BenchmarkNavRepository benchmarkNavRepository;

    @Autowired
    InvestorPortfolioHealthCheckupService investorPortfolioHealthCheckupService;

    @Autowired
    AmfiSchemeMasterService amfiSchemeMasterService;

    @GetMapping("/getBySchemeBenchmarkCodeOrderByNavDateDesc")
    public ResponseEntity<?> getschemeCamsProductCodesByCompany(@RequestParam String schemeBenchmarkCode)
    {
        try
        {
            List<BenchmarkNav> schemeCodes = benchmarkNavRepository.findBySchemeBenchmarkCodeOrderByNavDateDesc(schemeBenchmarkCode);

            if (schemeCodes == null || schemeCodes.isEmpty())
            {
                return AmfiUtils.commonResponse("No schemes found", HttpStatus.BAD_REQUEST);
            }

            return ResponseEntity.ok(schemeCodes);

        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return AmfiUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getByOldSchemeBenchmarkCodeOrderByNavDateDesc")
    public ResponseEntity<?> getByOldSchemeBenchmarkCodeOrderByNavDateDesc(@RequestParam String schemeBenchmarkCode)
    {
        try
        {
            List<BenchmarkNavOld> schemeCodes = benchmarkNavOldRepository.findByOldSchemeBenchmarkCodeOrderByNavDateDesc(schemeBenchmarkCode);

            if (schemeCodes == null || schemeCodes.isEmpty())
            {
                return AmfiUtils.commonResponse("No schemes found", HttpStatus.BAD_REQUEST);
            }

            return ResponseEntity.ok(schemeCodes);

        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return AmfiUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getInvestorPortfolioHealthCheckup")
    public ResponseEntity<?> getInvestorPortfolioHealthCheckup()
    {
        try
        {
            List<InvestorPortfolioHealthCheckup> schemeCodes = investorPortfolioHealthCheckupService.getRatingList();

            if (schemeCodes == null || schemeCodes.isEmpty())
            {
                return AmfiUtils.commonResponse("No schemes found", HttpStatus.BAD_REQUEST);
            }

            return ResponseEntity.ok(schemeCodes);

        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return AmfiUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getActiveWithIsinNo")
    public ResponseEntity<?> getActiveWithIsinNo()
    {
        try
        {
            List<AmfiSchemeMaster> schemeCodes = amfiSchemeMasterService.getActiveWithIsinNo();

            if (schemeCodes == null || schemeCodes.isEmpty())
            {
                return AmfiUtils.commonResponse("No schemes found", HttpStatus.BAD_REQUEST);
            }

            return ResponseEntity.ok(schemeCodes);

        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return AmfiUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/findSchemeAmfiMaster")
    public ResponseEntity<?> findSchemeAmfiMaster(@RequestParam String scheme_amfi)
    {
        try
        {
            System.out.println("schemamfi = " + scheme_amfi);
            List<AmfiSchemeMaster> schemeCodes = amfiSchemeMasterService.findSchemeAmfiMaster(scheme_amfi);
            System.out.println("schemeCodes = " + schemeCodes);
            if (schemeCodes == null || schemeCodes.isEmpty())
            {
                return AmfiUtils.commonResponse("No schemes found", HttpStatus.BAD_REQUEST);
            }
            return ResponseEntity.ok(schemeCodes);
        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return AmfiUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/autoSuggestAllMfSchemes")
    public ResponseEntity<?> autoSuggestAllMfSchemes(@RequestParam String keyword,@RequestParam String category,@RequestParam String amc)
    {
        try
        {
            List<Object[]> schemeCodes = amfiSchemeMasterService.autoSuggestAllMfSchemes(keyword,category,amc);

            if (schemeCodes == null || schemeCodes.isEmpty())
            {
                return AmfiUtils.commonResponse("No schemes found", HttpStatus.BAD_REQUEST);
            }
            return ResponseEntity.ok(schemeCodes);
        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return AmfiUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
