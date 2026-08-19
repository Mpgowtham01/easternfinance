package com.amfi.controller.nse;

import com.amfi.model.AmfiLatestNav;
import com.amfi.model.AmfiMfNav;
import com.amfi.model.AmfiSchemeMaster;
import com.amfi.response.StatusMessage;
import com.amfi.service.AmfiLatestNavService;
import com.amfi.service.AmfiMfNavService;
import com.amfi.service.AmfiSchemeMasterService;
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


@Hidden
@RestController
@SecurityRequirement(name = "bearerAuth")
public class AmfiFeignClientNseController
{
    @Autowired
    AmfiSchemeMasterService amfiSchemeMasterService;

    @Autowired
    AmfiLatestNavService amfiLatestNavService;

    @Autowired
    AmfiMfNavService amfiMfNavService;

    @GetMapping("/getSchemeCamsProductCodesByCompany")
    public ResponseEntity<?> getschemeCamsProductCodesByCompany(@RequestParam String scheme_company)
    {
        try
        {
            List<String> schemeCodes = amfiSchemeMasterService.getCamsProductCodesByCompany(scheme_company);

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

    @GetMapping("/findBySchemeAmfiAndActive")
    public ResponseEntity<?> findBySchemeAmfiAndActive(@RequestParam String scheme_company)
    {
        try
        {
            List<AmfiSchemeMaster> schemeCodes = amfiSchemeMasterService.getActiveSchemesByAmfi(scheme_company);

            if (schemeCodes == null || schemeCodes.isEmpty())
            {
                return AmfiUtils.commonResponse("No schemes found", HttpStatus.BAD_REQUEST);
            }

            return ResponseEntity.ok().body(schemeCodes);

        }catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return AmfiUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getBySchemeCamsProductcode")
    public ResponseEntity<?> getBySchemeCamsProductcode(@RequestParam String schemeCamsProductcode)
    {
        try
        {
            AmfiSchemeMaster schemeCodes = amfiSchemeMasterService.getFirstByCamsProductCode(schemeCamsProductcode);

            if (schemeCodes == null)
            {
                return AmfiUtils.commonResponse("No schemes found", HttpStatus.BAD_REQUEST);
            }

            return ResponseEntity.ok().body(schemeCodes);

        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return AmfiUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getBySchemeAmfiCodeAndActive")
    public ResponseEntity<?> getBySchemeAmfiCodeAndActive(@RequestParam String schemeAmfiCode)
    {
        try
        {
            List<AmfiSchemeMaster> schemeCodes = amfiSchemeMasterService.getActiveSchemesByAmfiCode(schemeAmfiCode);

            if (schemeCodes == null || schemeCodes.isEmpty())
            {
                return AmfiUtils.commonResponse("No schemes found", HttpStatus.BAD_REQUEST);
            }

            return ResponseEntity.ok().body(schemeCodes);

        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return AmfiUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getFirstBySchemeKarvyProductcode")
    public ResponseEntity<?> getFirstBySchemeKarvyProductcode(@RequestParam String schemeKarvyProductcode)
    {
        try
        {
            System.out.println("schemeKarvyProductcode= " + schemeKarvyProductcode);
            AmfiSchemeMaster schemeCodes = amfiSchemeMasterService.getFirstByKarvyProductCode(schemeKarvyProductcode);

            if (schemeCodes == null)
            {
                return AmfiUtils.commonResponse("No schemes found", HttpStatus.BAD_REQUEST);
            }
            System.out.println("schemeCodes= " + schemeCodes);
            return ResponseEntity.ok().body(schemeCodes);

        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return AmfiUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/findByLatestNav")
    public ResponseEntity<?> findByLatestNav(@RequestParam String schemeCode)
    {
        try
        {
            List<AmfiLatestNav> schemeCodes = amfiLatestNavService.getBySchemeCode(schemeCode);

            if (schemeCodes == null || schemeCodes.isEmpty())
            {
                return AmfiUtils.commonResponse("No schemes found", HttpStatus.BAD_REQUEST);
            }

            return ResponseEntity.ok().body(schemeCodes);

        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return AmfiUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/findByMfNav")
    public ResponseEntity<?> findByMfNav(@RequestParam String schemeCode)
    {
        try
        {
            List<Double> schemeCodes = amfiMfNavService.getNetAssetValuesBySchemeCode(schemeCode);

            if (schemeCodes == null || schemeCodes.isEmpty())
            {
                return AmfiUtils.commonResponse("No schemes found", HttpStatus.BAD_REQUEST);
            }

            return ResponseEntity.ok().body(schemeCodes);

        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return AmfiUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getTopBySchemeCodeOrderByNavDateDesc")
    public ResponseEntity<?> getTopBySchemeCodeOrderByNavDateDesc(@RequestParam String schemeCode)
    {
        try
        {
            AmfiMfNav schemeCodes = amfiMfNavService.getLatestNavBySchemeCode(schemeCode);

            if (schemeCodes == null)
            {
                return AmfiUtils.commonResponse("No schemes found", HttpStatus.BAD_REQUEST);
            }

            return ResponseEntity.ok().body(schemeCodes);

        } catch (Exception ex)
        {
            System.out.println("Exception Date & Time = " + new Date() + " & ERROR = " + ex.getMessage());
            ex.printStackTrace();
            return AmfiUtils.commonResponse(StatusMessage.ExceptionAPIMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
