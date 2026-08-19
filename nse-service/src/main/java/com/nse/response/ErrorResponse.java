package com.nse.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Error Response")
public class ErrorResponse
{
    @Schema(description = "HTTP status code with reason", example = "400")
    private Integer status;

    @Schema(description = "Short error keyword", example = "Bad Request")
    private String error;

    @Schema(description = "Detailed error message", example = "Bearer auth token cannot be empty.")
    private String message;

}
