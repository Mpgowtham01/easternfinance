package com.user.repository;

import com.user.model.AdvisorEmailSignature;
import com.user.model.BseNseKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface AdvisorEmailSignatureRepository extends JpaRepository<AdvisorEmailSignature, Long>
{
    @Query("FROM AdvisorEmailSignature a WHERE a.client_name = :clientName")
    AdvisorEmailSignature findByClientName(@Param("clientName") String clientName);
}