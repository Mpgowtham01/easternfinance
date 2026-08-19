package com.nse.repository;


import com.nse.model.NseLogModel;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NseLogRepository extends JpaRepository<NseLogModel, Integer> {

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO nse_log (client_name, user_id, username, mobile, title, description, content, ip, logtime, source) " +
            "VALUES (:clientName, :userId, :userName, :mobile, :title, :description, :content, :ip, NOW(), :source)", nativeQuery = true)
    void saveLog(
            @Param("clientName") String clientName,
            @Param("userId") Integer userId,
            @Param("userName") String userName,
            @Param("mobile") String mobile,
            @Param("title") String title,
            @Param("description") String description,
            @Param("content") String content,
            @Param("ip") String ip,
            @Param("source") String source
    );



}