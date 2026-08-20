//package com.user.repository;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//public interface UserBseNseDetailsRespository extends JpaRepository<UserBseNseDetails, Integer>
//{
//    @Query(value = "SELECT * FROM user_bse_nse_details WHERE user_id = :userId AND client_name = :clientName AND nse_active = 0", nativeQuery = true)
//    Optional<UserBseNseDetails> findInactiveNseByUserIdAndClientName(
//            @Param("userId") Integer userId,
//            @Param("clientName") String clientName
//    );
//
//    @Query("SELECT u FROM UserBseNseDetails u WHERE u.user_id = :userId AND u.nse_active = 0 ORDER BY u.id ASC")
//    List<UserBseNseDetails> findInactiveNseByUserId(@Param("userId") Integer userId);
//
//    @Query(value = "SELECT * FROM user_bse_nse_details WHERE nse_iin_number = :iinNumber AND client_name = :clientName", nativeQuery = true)
//    Optional<UserBseNseDetails> findInactiveNseByIinNumberAndClientName(
//            @Param("iinNumber") String iinNumber,
//            @Param("clientName") String clientName
//    );
//
//    @Query("FROM UserBseNseDetails u WHERE u.user_id = :userId  AND u.nse_iin_number = :nse_iin_number AND u.client_name = :clientName")
//    Optional<UserBseNseDetails> findByAllFields(
//            @Param("userId") Integer userId,
//            @Param("nse_iin_number") String nseIinNumber,
//            @Param("clientName") String clientName
//    );
//
//    @Query(value = "SELECT * FROM user_bse_nse_details WHERE broker_code = :brokerCode AND client_name = :clientName AND nse_active = 0", nativeQuery = true)
//    Optional<UserBseNseDetails> findInactiveNseByBrokerCodeAndClientName(
//            @Param("brokerCode") String brokerCode,
//            @Param("clientName") String clientName
//    );
//
//    @Query("FROM UserBseNseDetails u WHERE u.user_id = :user_id AND u.client_name = :client_name AND u.tax_status_code = :tax_status_code AND u.holding_nature_code = :holding_nature_code AND u.joint_holder_pan1 = :joint_holder_pan1 AND u.joint_holder_pan2 = :joint_holder_pan2")
//    Optional<UserBseNseDetails> getUserBseNseDetailsByAllFields(
//            @Param("user_id") Integer userId,
//            @Param("client_name") String clientName,
//            @Param("tax_status_code") String taxStatusCode,
//            @Param("holding_nature_code") String holdingNatureCode,
//            @Param("joint_holder_pan1") String jointHolderPan1,
//            @Param("joint_holder_pan2") String jointHolderPan2
//    );
//
//    @Query(value = "SELECT * FROM user_bse_nse_details WHERE user_id = :userId AND client_name = :clientName AND nse_active = 1", nativeQuery = true)
//    Optional<UserBseNseDetails> findUserByIdAndClientNameAndNseActive(@Param("userId") Integer userid, @Param("clientName") String clientName);
//
//    @Query(value = "SELECT * FROM user_bse_nse_details WHERE nse_iin_number = :iinNumber AND client_name = :clientName", nativeQuery = true)
//    Optional<UserBseNseDetails> findNseByIinNumberAndClientName(
//            @Param("iinNumber") String iinNumber,
//            @Param("clientName") String clientName
//    );
//    @Query(value = "SELECT * FROM user_bse_nse_details WHERE user_id = :userId AND client_name = :clientName AND nse_active = 1", nativeQuery = true)
//    List<UserBseNseDetails> findUserByIdAndClientNameAndNseActives(@Param("userId") Integer userid, @Param("clientName") String clientName);
//
//    @Query(value = "SELECT * FROM user_bse_nse_details WHERE user_id = :userId AND client_name = :clientName AND nse_active = 0", nativeQuery = true)
//    List<UserBseNseDetails> findInactiveNseByUserIdAndClientNames(
//            @Param("userId") Integer userId,
//            @Param("clientName") String clientName
//    );
//
//    @Query(value = "SELECT * FROM user_bse_nse_details WHERE user_id = :userId AND client_name = :clientName and nse_active = 0", nativeQuery = true)
//    Optional<UserBseNseDetails> findInactiveNseByUserIdAndClientNameInActive(
//            @Param("userId") Integer userId,
//            @Param("clientName") String clientName
//    );
//
//    @Query(value = "SELECT * FROM user_bse_nse_details WHERE user_id = :userId AND client_name = :clientName AND nse_active = 1", nativeQuery = true)
//    List<UserBseNseDetails> findInactiveNseByUserIdAndClientname1(
//            @Param("userId") Integer userId,
//            @Param("clientName") String clientName
//    );
//
//    @Query("FROM UserBseNseDetails u WHERE u.user_id = :userId AND u.nse_iin_number = :bseClientCode AND u.client_name = :clientName")
//    List<UserBseNseDetails> findByUserIdAndBseClientCodeAndClientName(
//            @Param("userId") Integer userId,
//            @Param("bseClientCode") String bseClientCode,
//            @Param("clientName") String clientName
//    );
//
//    @Query("FROM UserBseNseDetails u " +
//            "WHERE u.user_id = :userId " +
//            "AND u.nse_iin_number = :bseClientCode " +
//            "AND u.broker_code = :brokerCode " +
//            "AND u.client_name = :clientName")
//    List<UserBseNseDetails> findByUserIdAndBseClientCodeAndBrokerCodeAndClientName(
//            @Param("userId") Integer userId,
//            @Param("bseClientCode") String bseClientCode,
//            @Param("brokerCode") String brokerCode,
//            @Param("clientName") String clientName
//    );
//
//    @Query("FROM UserBseNseDetails u " +
//            "WHERE u.nse_iin_number = :bseClientCode " +
//            "AND u.client_name = :clientName " +
//            "AND u.nse_active = 1")
//    List<UserBseNseDetails> findActiveBseByBseClientCodeAndClientName(
//            @Param("bseClientCode") String bseClientCode,
//            @Param("clientName") String clientName
//    );
//
//    @Query(value = "SELECT * FROM user_bse_nse_details WHERE user_id = :id AND client_name = :clientName", nativeQuery = true)
//    Optional<UserBseNseDetails> getUserRegDetailsByOnlineId(@Param("id") Integer id, @Param("clientName") String clientName);
//
//    @Query(value = "SELECT * FROM user_bse_nse_details WHERE user_id = :id AND client_name = :clientName", nativeQuery = true)
//    List<UserBseNseDetails> getUserRegDetailsByOnlineIdList(@Param("id") Integer id, @Param("clientName") String clientName);
//
//    @Query(value = "SELECT * FROM user_bse_nse_details WHERE user_id = :userId AND client_name = :clientName", nativeQuery = true)
//    List<UserBseNseDetails> findUserByIdAndClientNameActiveNse(@Param("userId") Integer userid, @Param("clientName") String clientName);
//
//    @Query(value = "SELECT * FROM user_bse_nse_details WHERE user_id = :userId AND client_name = :clientName AND nse_active = 0", nativeQuery = true)
//    List<UserBseNseDetails> getNseInactiveUserRegDetailsByUserIdAndClientName(@Param("userId") Integer userId, @Param("clientName") String clientName);
//
//    @Query(value = "SELECT * FROM user_bse_nse_details WHERE nse_iin_number = :nse_iin_number AND client_name = :clientName", nativeQuery = true)
//    List<UserBseNseDetails> getUserDetailsByIinNumberAndClientName(@Param("nse_iin_number") String nse_iin_number, @Param("clientName") String clientName);
//
//    @Query(value = "SELECT * FROM user_bse_nse_details WHERE nse_iin_number = :iinNumber AND broker_code = :broker_code AND client_name = :client_name AND user_id = :id", nativeQuery = true)
//    Optional<UserBseNseDetails> findNseByIinNumberAndBrokercode(
//            @Param("iinNumber") String iinNumber,
//            @Param("broker_code") String broker_code,
//            @Param("client_name") String client_name,
//            @Param("id") String id
//    );
//
//    @Query(value = "SELECT * FROM user_bse_nse_details WHERE nse_iin_number = :iinNumber AND broker_code = :broker_code AND client_name = :client_name AND id = :id", nativeQuery = true)
//    UserBseNseDetails findNseByIinNumberAndBrokercodeAndId(
//            @Param("iinNumber") String iinNumber,
//            @Param("broker_code") String broker_code,
//            @Param("client_name") String client_name,
//            @Param("id") String id
//    );
//
//    @Query(value = "SELECT * FROM user_bse_nse_details WHERE user_id = :user_id AND client_name = :client_name AND tax_status_code = :tax_status_code AND holding_nature_code = :holding_nature_code AND broker_code = :broker_code and nse_iin_number= :nse_iin_number", nativeQuery = true)
//    UserBseNseDetails getUserBseNseDetailsByAllFieldsForNse(
//            @Param("user_id") Integer userId,
//            @Param("client_name") String clientName,
//            @Param("tax_status_code") String taxStatusCode,
//            @Param("holding_nature_code") String holdingNatureCode,
//            @Param("broker_code") String broker_code,
//            @Param("nse_iin_number") String in_number
//    );
//
//    @Query(value = "SELECT * FROM user_bse_nse_details WHERE user_id = :userId AND client_name = :clientName AND pan = :pan AND nse_active = 0", nativeQuery = true)
//    List<UserBseNseDetails> getactiveUserRegDetailsByUserIdAndClientName(@Param("userId") Integer userId, @Param("clientName") String clientName, @Param("pan") String pan);
//
//}
