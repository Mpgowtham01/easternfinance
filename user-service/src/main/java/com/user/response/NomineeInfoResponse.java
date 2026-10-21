package com.user.response;

import com.user.dto.NomineeInfoDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "Nominee Information Response")
public class NomineeInfoResponse
{
    @Schema(description = "Nominee opt-in flag", example = "Y")
    public String nominee_opt = "";

    public List<NomineeInfoDTO> response = new ArrayList<>();
}
