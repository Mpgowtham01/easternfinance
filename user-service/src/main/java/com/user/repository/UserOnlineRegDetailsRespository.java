package com.user.repository;

import com.user.model.User;
import com.user.model.UsersOnlineRegDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserOnlineRegDetailsRespository extends JpaRepository<UsersOnlineRegDetails, Integer>
{

    @Query("SELECT u FROM UsersOnlineRegDetails u WHERE u.user_id = :userId")
    List<UsersOnlineRegDetails> findNseUserByUserId(@Param("userId") Integer userId);

    @Query("SELECT u FROM UsersOnlineRegDetails u WHERE u.user_id = :userId AND u.client_name = :clientName")
    Optional<UsersOnlineRegDetails> findNseUserByUserIdAndClientName(@Param("userId") Integer userId,@Param("clientName") String clientName);

    @Query("SELECT u FROM UsersOnlineRegDetails u WHERE u.user_id = :userId AND u.client_name = :clientName")
    List<UsersOnlineRegDetails> findListOfNseUserByUserIdAndClientName(@Param("userId") Integer userId,@Param("clientName") String clientName);

    @Query("SELECT u FROM UsersOnlineRegDetails u WHERE u.user_id = :userId AND u.nse_active = 0 ORDER BY u.id ASC")
    List<UsersOnlineRegDetails> findInactiveNseByUserId(@Param("userId") Integer userId);

    @Query(value = "SELECT * FROM users_online_reg_details WHERE nse_iin_number = :iinNumber AND client_name = :clientName", nativeQuery = true)
    Optional<UsersOnlineRegDetails> findInactiveNseByIinNumberAndClientName(
            @Param("iinNumber") String iinNumber,
            @Param("clientName") String clientName
    );

    @Query("FROM UsersOnlineRegDetails u WHERE u.user_id = :userId  AND u.nse_iin_number = :nse_iin_number AND u.client_name = :clientName")
    Optional<UsersOnlineRegDetails> findByAllFields(
            @Param("userId") Integer userId,
            @Param("nse_iin_number") String nseIinNumber,
            @Param("clientName") String clientName
    );

    @Query("SELECT u FROM UsersOnlineRegDetails u WHERE u.client_name = :clientName AND u.nse_iin_number = :nseIinNumber")
    List<UsersOnlineRegDetails> findByClientNameAndNseIinNumberList(@Param("clientName") String clientName, @Param("nseIinNumber") String nseIinNumber);

    @Query(value = "SELECT * FROM users_online_reg_details WHERE user_id = :userId AND client_name = :clientName and nse_active = 0", nativeQuery = true)
    Optional<UsersOnlineRegDetails> findInactiveNseByUserIdAndClientNameInActive(
            @Param("userId") Integer userId,
            @Param("clientName") String clientName
    );

    @Query(value = "SELECT * FROM users_online_reg_details WHERE user_id = :userId AND client_name = :clientName AND nse_active = 1", nativeQuery = true)
    List<UsersOnlineRegDetails> findInactiveNseByUserIdAndClientname1(
            @Param("userId") Integer userId,
            @Param("clientName") String clientName
    );

    @Query("FROM UsersOnlineRegDetails u WHERE u.user_id = :userId AND u.nse_iin_number = :bseClientCode AND u.client_name = :clientName")
    List<UsersOnlineRegDetails> findByUserIdAndBseClientCodeAndClientName(
            @Param("userId") Integer userId,
            @Param("bseClientCode") String bseClientCode,
            @Param("clientName") String clientName
    );

    @Query("FROM UsersOnlineRegDetails u " +
            "WHERE u.user_id = :userId " +
            "AND u.nse_iin_number = :bseClientCode " +
            "AND u.broker_code = :brokerCode " +
            "AND u.client_name = :clientName")
    List<UsersOnlineRegDetails> findByUserIdAndBseClientCodeAndBrokerCodeAndClientName(
            @Param("userId") Integer userId,
            @Param("bseClientCode") String bseClientCode,
            @Param("brokerCode") String brokerCode,
            @Param("clientName") String clientName
    );

    @Query("FROM UsersOnlineRegDetails u " +
            "WHERE u.nse_iin_number = :bseClientCode " +
            "AND u.client_name = :clientName " +
            "AND u.nse_active = 1")
    List<UsersOnlineRegDetails> findActiveBseByBseClientCodeAndClientName(
            @Param("bseClientCode") String bseClientCode,
            @Param("clientName") String clientName
    );

    @Query(value = "SELECT * FROM users_online_reg_details WHERE user_id = :id AND client_name = :clientName", nativeQuery = true)
    Optional<UsersOnlineRegDetails> getUserRegDetailsByOnlineId(@Param("id") Integer id, @Param("clientName") String clientName);

    @Query("FROM UsersOnlineRegDetails u WHERE u.user_id = :user_id AND u.client_name = :client_name AND u.tax_status_code = :tax_status_code AND u.holding_nature_code = :holding_nature_code AND u.joint_holder_pan1 = :joint_holder_pan1 AND u.joint_holder_pan2 = :joint_holder_pan2")
    Optional<UsersOnlineRegDetails> getUserBseNseDetailsByAllFields(
            @Param("user_id") Integer userId,
            @Param("client_name") String clientName,
            @Param("tax_status_code") String taxStatusCode,
            @Param("holding_nature_code") String holdingNatureCode,
            @Param("joint_holder_pan1") String jointHolderPan1,
            @Param("joint_holder_pan2") String jointHolderPan2
    );

    @Query(value = "SELECT * FROM users_online_reg_details WHERE nse_iin_number = :iinNumber AND broker_code = :broker_code AND client_name = :client_name AND user_id = :id", nativeQuery = true)
    Optional<UsersOnlineRegDetails> findNseByIinNumberAndBrokercode(
            @Param("iinNumber") String iinNumber,
            @Param("broker_code") String broker_code,
            @Param("client_name") String client_name,
            @Param("id") String id
    );

    @Query(value = "SELECT * FROM users_online_reg_details WHERE user_id = :userId AND client_name = :clientName AND nse_active = 1", nativeQuery = true)
    List<UsersOnlineRegDetails> findUserByIdAndClientNameAndNseActives(@Param("userId") Integer userid, @Param("clientName") String clientName);

    @Query(value = "SELECT * FROM users_online_reg_details WHERE nse_iin_number = :iinNumber AND client_name = :clientName", nativeQuery = true)
    Optional<UsersOnlineRegDetails> findNseByIinNumberAndClientName(
            @Param("iinNumber") String iinNumber,
            @Param("clientName") String clientName
    );

    @Query(value = "SELECT * FROM users_online_reg_details WHERE user_id = :userId", nativeQuery = true)
    Optional<UsersOnlineRegDetails> findUSerByIdAndActive(@Param("userId") Integer userId);

    @Query(value = "SELECT * FROM users_online_reg_details WHERE user_id = :userId AND client_name = :clientName AND nse_active = 0", nativeQuery = true)
    List<UsersOnlineRegDetails> getNseInactiveUserRegDetailsByUserIdAndClientName(@Param("userId") Integer userId, @Param("clientName") String clientName);

    @Query(value = "SELECT * FROM users_online_reg_details WHERE nse_iin_number = :nse_iin_number AND client_name = :clientName", nativeQuery = true)
    List<UsersOnlineRegDetails> getUserDetailsByIinNumberAndClientName(@Param("nse_iin_number") String nse_iin_number, @Param("clientName") String clientName);

    @Query(value = "SELECT * FROM users_online_reg_details WHERE user_id = :userId AND client_name = :clientName", nativeQuery = true)
    List<UsersOnlineRegDetails> findByUseridAndClientName(@Param("userId") Integer userid, @Param("clientName") String clientName);

    @Query("SELECT u FROM UsersOnlineRegDetails u WHERE u.client_name = :clientName AND u.nse_iin_number = :nseIinNumber AND u.broker_code = :arn_number")
    UsersOnlineRegDetails findByClientNameAndNseIinNumberAndActive(@Param("clientName") String clientName, @Param("nseIinNumber") String nseIinNumber, @Param("arn_number") String arn_number);

    @Query(value = "SELECT * FROM users_online_reg_details WHERE user_id = :userId AND broker_code = :broker_code AND online_flag = :online_flag AND nse_iin_number = :nse_iin_number", nativeQuery = true)
    UsersOnlineRegDetails findByUserIdAndOnlineCodeAndIinNumber(
            @Param("userId") Integer userId,
            @Param("online_flag") String online_flag,
            @Param("broker_code") String broker_code,
            @Param("nse_iin_number") String iin_number
    );

    @Query(value = "SELECT * FROM users_online_reg_details WHERE user_id = :userId AND broker_code = :broker_code AND online_flag = :online_flag", nativeQuery = true)
    UsersOnlineRegDetails findByUserIdAndOnlineCode(
            @Param("userId") Integer userId,
            @Param("online_flag") String online_flag,
            @Param("broker_code") String broker_code
    );

    @Query("SELECT u FROM UsersOnlineRegDetails u WHERE u.client_name = :clientName AND u.nse_iin_number = :nseIinNumber")
    Optional<UsersOnlineRegDetails> findByClientNameAndNseIinNumber(@Param("clientName") String clientName, @Param("nseIinNumber") String nseIinNumber);

    @Query("FROM UsersOnlineRegDetails u WHERE u.user_id = :user_id AND u.client_name = :client_name AND u.tax_status_code = :tax_status_code AND u.holding_nature_code = :holding_nature_code AND u.broker_code = :broker_code and u.online_flag= :online_flag and u.nse_active=0")
    Optional<UsersOnlineRegDetails> getUserBseNseDetailsByAllFieldsFOrNse(
            @Param("user_id") Integer userId,
            @Param("client_name") String clientName,
            @Param("tax_status_code") String taxStatusCode,
            @Param("holding_nature_code") String holdingNatureCode,
            @Param("broker_code") String broker_code,
            @Param("online_flag") String online_flag
    );

    @Query(value = "SELECT * FROM users_online_reg_details WHERE nse_iin_number = :iinNumber AND broker_code = :broker_code AND client_name = :client_name AND id = :id", nativeQuery = true)
    UsersOnlineRegDetails findNseByIinNumberAndBrokercodeAndId(
            @Param("iinNumber") String iinNumber,
            @Param("broker_code") String broker_code,
            @Param("client_name") String client_name,
            @Param("id") String id
    );

    @Query("FROM UsersOnlineRegDetails u WHERE u.id = :user_id AND u.client_name = :client_name AND u.tax_status_code = :tax_status_code AND u.holding_nature_code = :holding_nature_code AND u.broker_code = :broker_code and nse_iin_number= :nse_iin_number")
    Optional<UsersOnlineRegDetails> getUserBseNseDetailsByAllFieldsForNse(
            @Param("user_id") Integer userId,
            @Param("client_name") String clientName,
            @Param("tax_status_code") String taxStatusCode,
            @Param("holding_nature_code") String holdingNatureCode,
            @Param("broker_code") String broker_code,
            @Param("nse_iin_number") String in_number
    );

    @Query("SELECT u FROM UsersOnlineRegDetails u WHERE u.nse_iin_number = :iinNumber AND u.client_name = :clientName AND u.broker_code = :broker_code AND u.user_id = :userid")
    List<UsersOnlineRegDetails> findNseByIinNumberAndClientNameBrokerCode(
            @Param("iinNumber") String iinNumber,
            @Param("clientName") String clientName,
            @Param("broker_code") String broker_code,
            @Param("userid") String userid
    );

}
