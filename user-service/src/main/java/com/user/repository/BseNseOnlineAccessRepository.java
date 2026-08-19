package com.user.repository;

import com.user.model.BseNseOnlineAccess;
import org.springframework.data.jpa.repository.JpaRepository;


public interface BseNseOnlineAccessRepository extends JpaRepository<BseNseOnlineAccess, Long> {
    BseNseOnlineAccess findByClientNameAndBrokerCode(String clientName, String brokerCode);
}