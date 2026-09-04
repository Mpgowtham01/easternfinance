package com.user.repository;

import com.user.model.BseNseKey;
import com.user.model.BseNseOnlineAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BseNseKeyRepository extends JpaRepository<BseNseKey, Long>
{

    BseNseKey findByClientName(String client_name);

    BseNseKey findByClientNameAndBrokerCode(String clientName, String brokerCode);

    @Query("SELECT DISTINCT m.brokerCode FROM BseNseKey m WHERE m.clientName = :clientName")
    List<String> findDistinctBrokerCodesByClientName(@Param("clientName") String clientName);

}
