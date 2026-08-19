package com.nse.response;

import lombok.Data;

@Data
public class NseSchemeDetailsResponse
{
    private Integer user_id;
    private String amc_code;
    private String amc_name;
    private String folio_no;
    private String registrar;
    private String scheme_category;
    private String scheme_code;
    private String dividend_option;
    private String scheme_option_code;
    private String scheme_amfi_code;
    private String scheme_name;
    private Double total_units;
    private Double load_free_units;
    private Double current_value;


    public Integer getUser_id() {
        return user_id;
    }
    public void setUser_id(Integer user_id) {
        this.user_id = user_id;
    }
    public String getAmc_code() {
        return amc_code;
    }
    public void setAmc_code(String amc_code) {
        this.amc_code = amc_code;
    }
    public String getAmc_name() {
        return amc_name;
    }
    public void setAmc_name(String amc_name) {
        this.amc_name = amc_name;
    }
    public String getFolio_no() {
        return folio_no;
    }
    public void setFolio_no(String folio_no) {
        this.folio_no = folio_no;
    }
    public String getRegistrar() {
        return registrar;
    }
    public void setRegistrar(String registrar) {
        this.registrar = registrar;
    }
    public String getScheme_category() {
        return scheme_category;
    }
    public void setScheme_category(String scheme_category) {
        this.scheme_category = scheme_category;
    }
    public String getScheme_code() {
        return scheme_code;
    }
    public void setScheme_code(String scheme_code) {
        this.scheme_code = scheme_code;
    }
    public String getDividend_option() {
        return dividend_option;
    }
    public void setDividend_option(String dividend_option) {
        this.dividend_option = dividend_option;
    }
    public String getScheme_option_code() {
        return scheme_option_code;
    }
    public void setScheme_option_code(String scheme_option_code) {
        this.scheme_option_code = scheme_option_code;
    }
    public String getScheme_amfi_code() {
        return scheme_amfi_code;
    }
    public void setScheme_amfi_code(String scheme_amfi_code) {
        this.scheme_amfi_code = scheme_amfi_code;
    }
    public String getScheme_name() {
        return scheme_name;
    }
    public void setScheme_name(String scheme_name) {
        this.scheme_name = scheme_name;
    }
    public Double getTotal_units() {
        return total_units;
    }
    public void setTotal_units(Double total_units) {
        this.total_units = total_units;
    }
    public Double getLoad_free_units() {
        return load_free_units;
    }
    public void setLoad_free_units(Double load_free_units) {
        this.load_free_units = load_free_units;
    }
    public Double getCurrent_value() {
        return current_value;
    }
    public void setCurrent_value(Double current_value) {
        this.current_value = current_value;
    }
}