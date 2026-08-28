package com.user.repository;

import com.user.model.Cart;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer>
{
    @Query("FROM Cart c WHERE c.user_id = :userId AND c.client_name = :clientName AND c.purchase_type = :purchaseType AND c.investor_code = :investorCode AND c.vendor = :vendor AND c.active = true ORDER BY c.id DESC")
    List<Cart> findActiveCarts(
            @Param("userId") Integer userId,
            @Param("clientName") String clientName,
            @Param("purchaseType") String purchaseType,
            @Param("investorCode") String investorCode,
            @Param("vendor") String vendor
    );

    @Query("FROM Cart c WHERE c.id = :id AND c.client_name = :clientName ORDER BY c.id DESC")
    Optional<Cart> findByIdAndClientNameOrderByIdDesc(
            @Param("id") Integer id,
            @Param("clientName") String clientName
    );

    @Query("FROM Cart c WHERE c.user_id = :userId AND c.investor_code = :investorCode AND c.folio_no = :folioNo AND c.purchase_type = :purchaseType AND (c.scheme_name = :schemeName OR c.scheme_amfi_short_name = :schemeName) AND c.scheme_reinvest_tag = :schemeReinvestTag AND c.client_name = :clientName AND c.active = true ORDER BY c.id DESC")
    List<Cart> findCartByAllParams(
            @Param("userId") Integer userId,
            @Param("investorCode") String investorCode,
            @Param("folioNo") String folioNo,
            @Param("purchaseType") String purchaseType,
            @Param("schemeName") String schemeName,
            @Param("schemeReinvestTag") String schemeReinvestTag,
            @Param("clientName") String clientName
    );

    @Query("FROM Cart c WHERE c.user_id = :userId AND c.client_name = :clientName AND c.payment_id = :paymentId AND c.active = false ORDER BY c.id DESC")
    List<Cart> findInactiveCartsByUserIdAndClientNameAndPaymentId(
            @Param("userId") Integer userId,
            @Param("clientName") String clientName,
            @Param("paymentId") String paymentId
    );

    @Modifying
    @Transactional
    @Query("DELETE FROM Cart c WHERE c.id = :id AND c.user_id = :userId AND c.client_name = :clientName")
    void deleteByIdAndUserIdAndClientName(
            @Param("id") Integer id,
            @Param("userId") Integer userId,
            @Param("clientName") String clientName
    );


    @Modifying
    @Transactional
    @Query("DELETE FROM Cart c WHERE c.user_id = :userId AND c.purchase_type = :purchaseType AND c.client_name = :clientName AND c.vendor = :vendor AND c.active = true")
    void deleteActiveCartsByUserIdAndPurchaseTypeAndClientName(
            @Param("userId") Integer userId,
            @Param("purchaseType") String purchaseType,
            @Param("clientName") String clientName,
            @Param("vendor") String vendor
    );

    @Query("FROM Cart c WHERE c.user_id = :userId AND c.client_name = :clientName AND c.purchase_type = :purchaseType AND c.vendor = :vendor AND c.active = false AND c.status = 'SUCCESS' ORDER BY c.id DESC")
    List<Cart> findInactiveSuccessfulCarts(
            @Param("userId") Integer userId,
            @Param("clientName") String clientName,
            @Param("purchaseType") String purchaseType,
            @Param("vendor") String vendor
    );

    @Query("FROM Cart c WHERE c.user_id = :userId AND c.client_name = :clientName AND c.vendor = :vendor AND c.purchase_type = :purchaseType AND c.active = true ORDER BY c.id DESC")
    List<Cart> findActiveCartsByUserIdAndClientNameAndPurchaseType(
            @Param("userId") Integer userId,
            @Param("clientName") String clientName,
            @Param("purchaseType") String purchaseType,
            @Param("vendor") String vendor
    );
    @Query("SELECT c FROM Cart c WHERE c.id IN :ids AND c.active = true")
    List<Cart> findAllCartsBasedOnIds(@Param("ids") List<Integer> ids);

    @Query("FROM Cart c WHERE c.user_id = :userId AND c.client_name = :clientName AND c.vendor = :vendor AND c.active = true ORDER BY c.id DESC")
    List<Cart> findAllActiveCartsByUserId(
            @Param("userId") Integer userId,
            @Param("clientName") String clientName,
            @Param("vendor") String vendor
    );

    @Query("FROM Cart c WHERE c.user_id = :userId AND c.client_name = :clientName AND c.vendor = :vendor AND c.purchase_type = :purchase_type AND c.active = true ORDER BY c.id DESC")
    List<Cart> findAllActiveCartsByUserIdAndClientCodeWithoutBrokerCode(
            @Param("userId") Integer userId,
            @Param("clientName") String clientName,
            @Param("vendor") String vendor,
            @Param("purchase_type") String purchase_type
    );

    @Query("FROM Cart c WHERE c.user_id = :userId AND c.client_name = :clientName AND c.vendor = :vendor AND c.purchase_type = :purchase_type AND broker_code = :broker_code AND c.active = true ORDER BY c.id DESC")
    List<Cart> findAllActiveCartsByUserIdAndClientCode(
            @Param("userId") Integer userId,
            @Param("clientName") String clientName,
            @Param("vendor") String vendor,
            @Param("purchase_type") String purchase_type,
            @Param("broker_code") String broker_code
    );

    @Query("FROM Cart c WHERE c.user_id = :userId " +
            "AND c.investor_code = :investorCode " +
            "AND c.folio_no = :folioNo " +
            "AND c.purchase_type = :purchaseType " +
            "AND (c.scheme_name = :schemeName OR c.scheme_amfi_short_name = :schemeName) " +
            "AND (c.to_scheme_name = :toSchemeName OR c.to_scheme_amfi_short_name = :toSchemeName) " +
            "AND c.scheme_reinvest_tag = :schemeReinvestTag " +
            "AND c.client_name = :clientName " +
            "AND c.active = true " +
            "ORDER BY c.id DESC")
    List<Cart> findCartByAllParamsForNse(
            @Param("userId") Integer userId,
            @Param("investorCode") String investorCode,
            @Param("folioNo") String folioNo,
            @Param("purchaseType") String purchaseType,
            @Param("schemeName") String schemeName,
            @Param("schemeReinvestTag") String schemeReinvestTag,
            @Param("toSchemeName") String toSchemeName,
            @Param("clientName") String clientName
    );

    @Query("FROM Cart c WHERE c.id = :cart_id ORDER BY c.id DESC")
    List<Cart> findActiveCartsById(
            @Param("cart_id") Integer cart_id
    );

}
