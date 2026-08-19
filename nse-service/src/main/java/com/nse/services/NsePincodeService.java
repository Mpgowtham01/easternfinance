package com.nse.services;

import com.nse.model.NsePincode;
import com.nse.repository.NsePincodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class NsePincodeService
{
    @Autowired
    NsePincodeRepository nsePincodeRepository;

    // Method 1: Uses @Query with Optional
    public Optional<NsePincode> getPincodeDetails(String pincode)
    {
        return nsePincodeRepository.findByPincode(pincode);
    }
}
