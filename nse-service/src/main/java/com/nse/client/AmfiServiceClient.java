package com.nse.client;


import com.nse.dto.amfi.AmfiLatestNavDto;
import com.nse.dto.amfi.AmfiMfNavDto;
import com.nse.dto.amfi.AmfiSchemeMasterDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Objects;

@FeignClient(name = "amfi-service", url = "${amfi.service.base-url}")
public interface AmfiServiceClient {

    @GetMapping("/getSchemeCamsProductCodesByCompany")
    List<AmfiSchemeMasterDTO> getschemeCamsProductCodesByCompany(
            @RequestParam("scheme_company") String scheme_company,@RequestHeader("Authorization") String token
    );

    @GetMapping("/getSchemeCamsProductCodesByCompany")
    List<String> getschemeCamsProductCodesByCompanys(
            @RequestParam("scheme_company") String scheme_company,@RequestHeader("Authorization") String token
    );

    @GetMapping("/findBySchemeAmfiAndActive")
    List<AmfiSchemeMasterDTO> findBySchemeAmfiAndActive(
            @RequestParam("scheme_company") String scheme_company,@RequestHeader("Authorization") String token
    );

    @GetMapping("/findByLatestNav")
    List<AmfiLatestNavDto> findByLatestNav(
            @RequestParam("schemeCode") String schemeCode,@RequestHeader("Authorization") String token
    );

    @GetMapping("/findByMfNav")
    List<Double> findByMfNav(
            @RequestParam("schemeCode") String schemeCode,@RequestHeader("Authorization") String token
    );

    @GetMapping("/getBySchemeCamsProductcode")
    AmfiSchemeMasterDTO getBySchemeCamsProductcode(
            @RequestParam("schemeCamsProductcode") String schemeCode,@RequestHeader("Authorization") String token
    );

    @GetMapping("/getTopBySchemeCodeOrderByNavDateDesc")
    List<AmfiMfNavDto> getTopBySchemeCodeOrderByNavDateDesc(
            @RequestParam("schemeCode") String schemeCode,@RequestHeader("Authorization") String token
    );

    @GetMapping("/getFirstBySchemeKarvyProductcode")
    AmfiSchemeMasterDTO getFirstBySchemeKarvyProductcode(
            @RequestParam("schemeKarvyProductcode") String schemeKarvyProductcode,@RequestHeader("Authorization") String token
    );


    @GetMapping("/getBySchemeAmfiCodeAndActive")
    List<AmfiSchemeMasterDTO> getBySchemeAmfiCodeAndActive(
            @RequestParam("schemeAmfiCode") String schemeAmfiCode,@RequestHeader("Authorization") String token
    );

    @GetMapping("/findSchemeAmfiMaster")
    List<AmfiSchemeMasterDTO> findSchemeAmfiMaster(
            @RequestParam("scheme_amfi") String scheme_amfi,@RequestHeader("Authorization") String token
    );

    @GetMapping("/getActiveWithIsinNo")
    List<AmfiSchemeMasterDTO> getActiveWithIsinNo(
            @RequestHeader("Authorization") String token
    );

    @GetMapping("/autoSuggestAllMfSchemes")
    List<Object[]> autoSuggestAllMfSchemes(@RequestParam("keyword") String keyword,@RequestParam("category") String category,@RequestParam("amc") String amc,@RequestHeader("Authorization") String token);

}
