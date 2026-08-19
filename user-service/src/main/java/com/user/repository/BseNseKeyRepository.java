package com.user.repository;

import com.user.model.BseNseKey;
import com.user.model.BseNseOnlineAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BseNseKeyRepository extends JpaRepository<BseNseKey, Long>
{

    BseNseKey findByClientName(String client_name);

    BseNseKey findByClientNameAndBrokerCode(String clientName, String brokerCode);

}
