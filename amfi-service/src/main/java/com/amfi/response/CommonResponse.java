package com.amfi.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Common Response")
public class CommonResponse
{
    @Schema(description = "HTTP status code with reason", example = "200")
    private Integer status;

    @Schema(description = "Short error keyword", example = "Success")
    private String status_msg;

    @Schema(description = "Detailed error message", example = "Bearer auth token cannot be empty.")
    private String message;

}