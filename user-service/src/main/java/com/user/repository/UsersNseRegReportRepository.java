package com.user.repository;


import com.user.model.UsersNseRegReport;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;


public interface UsersNseRegReportRepository extends JpaRepository<UsersNseRegReport, Long> {
    @Query("FROM UsersNseRegReport u WHERE u.iin_number = :iin_number AND u.client_name = :client_name")
    Optional<UsersNseRegReport> findFirstByIin_numberAndClient_name(@Param("iin_number") String iin_number,
                                                                    @Param("client_name") String client_name);

}