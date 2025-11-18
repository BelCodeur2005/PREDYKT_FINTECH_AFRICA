// ============================================
// DTO: ImportResultResponse
// ============================================
package com.predykt.accounting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResultResponse {
    private Integer totalRows;
    private Integer successCount;
    private Integer errorCount;
    private String message;
    private List<String> errors;
}