package com.nse.repository;

import com.nse.model.NsePincode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NsePincodeRepository extends JpaRepository<NsePincode, Integer>
{
    @Query(value = "SELECT * FROM nse_pincode WHERE pincode = :pincode LIMIT 1", nativeQuery = true)
    Optional<NsePincode> findByPincode(@Param("pincode") String pincode);
}
