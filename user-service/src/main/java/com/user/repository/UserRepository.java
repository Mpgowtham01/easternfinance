package com.user.repository;

import com.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer>
{

    @Query(value = "SELECT * FROM users WHERE id = :userId", nativeQuery = true)
    Optional<User> findUSerByIdAndActive(@Param("userId") Integer userId);

    @Query(value = "SELECT * FROM users WHERE nse_iin_number = :nse_iin_number AND client_name = :clientName", nativeQuery = true)
    List<User> getUserDetailsByIinNumberAndClientName(@Param("nse_iin_number") String nse_iin_number, @Param("clientName") String clientName);

    @Query("SELECT u FROM User u WHERE u.pan = :pan AND u.name = :name AND u.client_name = :clientName")
    List<User> findByPanAndNameAndActiveSourceAndClientName(
            @Param("pan") String pan,
            @Param("name") String name,
            @Param("clientName") String clientName
    );

    @Query(value = "SELECT DISTINCT id FROM users WHERE client_name = :clientName AND type_id = 1 AND id NOT IN (:excludedIds)", nativeQuery = true)
    List<Integer> findDistinctUserIdsByClientNameAndTypeIdNotInExcludedIds(
            @Param("clientName") String clientName,
            @Param("excludedIds") List<Integer> excludedIds
    );

    @Query(value = "SELECT * FROM users WHERE id IN (:ids) AND client_name = :clientName", nativeQuery = true)
    List<User> findByIdInAndClientNameNative(
            @Param("ids") List<Integer> ids,
            @Param("clientName") String clientName
    );

    @Query("SELECT u FROM User u WHERE u.pan = :pan AND u.name = :name AND u.client_name = :clientName")
    List<User> findByPanAndNameAndClientName(
            @Param("pan") String pan,
            @Param("name") String name,
            @Param("clientName") String clientName
    );

    @Query(value = "SELECT * FROM users WHERE client_name = :clientName AND active = 1  AND pan = :pan AND name= :name ", nativeQuery = true)
    Optional<User> findByPanNameAndClientName(@Param("pan") String pan, @Param("name") String name, @Param("clientName") String clientName);

    @Query(value = "SELECT * FROM users WHERE client_name = :clientName AND active = 1  AND guard_pan = :pan AND name= :name ", nativeQuery = true)
    Optional<User> findByGuardPanNameAndClientName(@Param("pan") String pan, @Param("name") String name, @Param("clientName") String clientName);

    @Query("SELECT u FROM User u WHERE u.guard_pan = :guard_pan AND u.name = :name AND u.client_name = :clientName")
    List<User> findByGuardPanAndNameAndClientName(
            @Param("guard_pan") String guard_pan,
            @Param("name") String name,
            @Param("clientName") String clientName
    );

    //
//    @Query("SELECT u FROM User u WHERE u.client_name = :clientName AND u.nse_iin_number = :nseIinNumber")
//    Optional<User> findByClientNameAndNseIinNumber(@Param("clientName") String clientName, @Param("nseIinNumber") String nseIinNumber);
//
//    @Query(value = "SELECT * FROM users WHERE id = :userId AND client_name = :clientName AND active = 1", nativeQuery = true)
//    Optional<User> findUserByIdAndClientNameAndActive(@Param("userId") Integer userid, @Param("clientName") String clientName);
//
//    @Query("SELECT u FROM User u WHERE u.client_name = :clientName AND u.nse_iin_number = :nseIinNumber")
//    List<User> findByClientNameAndNseIinNumberList(@Param("clientName") String clientName, @Param("nseIinNumber") String nseIinNumber);
//
//    @Query("SELECT u FROM User u WHERE u.id = :id AND u.nse_iin_number = :nseIinNumber")
//    Optional<User> findUserByIdAndNseIinNumber(@Param("id") Integer id, @Param("nseIinNumber") String nseIinNumber);
//


//
//    @Query(value = "SELECT * FROM users WHERE id = :userId AND client_name = :clientName", nativeQuery = true)
//    Optional<User> findUserByIdAndClientName(@Param("userId") Integer userid, @Param("clientName") String clientName);
//
//    @Query(value = "SELECT * FROM users WHERE id = :userId AND client_name = :clientName AND active = 1", nativeQuery = true)
//    List<User> findByUserByIdAndClientNameAndActive(@Param("userId") Integer userid, @Param("clientName") String clientName);
//
//    @Query(value = "SELECT * FROM users WHERE id = :userId AND client_name = :clientName AND active = 1", nativeQuery = true)
//    User findByUserByIdAndClientNameAndActive1(@Param("userId") Integer userid, @Param("clientName") String clientName);
//

//

//
//    @Query(value = "SELECT * FROM users WHERE id = :userId AND client_name = :clientName AND active = 1", nativeQuery = true)
//    User findByUseridAndClientName(@Param("userId") Integer userid, @Param("clientName") String clientName);
//
//    @Query(value = "SELECT * FROM users WHERE id = :userId AND client_name = :clientName", nativeQuery = true)
//    List<User> getUserBseNseDetailsByUserID(
//            @Param("userId") Integer userid,
//            @Param("clientName") String clientName
//    );
//
//    @Query("SELECT u FROM User u WHERE u.broker_code = :broker_code AND u.nse_iin_number = :nseIinNumber AND u.client_name = :client_name AND u.id = :id")
//    Optional<User> findBybrokercodeAndNseIinNumber(@Param("broker_code") String broker_code, @Param("nseIinNumber") String nseIinNumber,
//                                                   @Param("client_name") String client_name,@Param("id") String id);
//
//    @Query("SELECT u FROM User u WHERE u.broker_code = :broker_code AND u.nse_iin_number = :nseIinNumber AND u.client_name = :client_name AND u.id = :id")
//    User findBybrokercodeAndNseIinNumberAndId(@Param("broker_code") String broker_code, @Param("nseIinNumber") String nseIinNumber,
//                                                 @Param("client_name") String client_name, @Param("id") Integer id);
//
//    @Query("FROM User u WHERE u.id = :user_id AND u.client_name = :client_name AND u.tax_status_code = :tax_status_code AND u.holding_nature_code = :holding_nature_code AND u.broker_code = :broker_code and nse_iin_number= :nse_iin_number")
//    Optional<User> getUserBseNseDetailsByAllFieldsForNse(
//            @Param("user_id") Integer userId,
//            @Param("client_name") String clientName,
//            @Param("tax_status_code") String taxStatusCode,
//            @Param("holding_nature_code") String holdingNatureCode,
//            @Param("broker_code") String broker_code,
//            @Param("nse_iin_number") String in_number
//    );
//

}
