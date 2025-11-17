package com.predykt.accounting.mapper;

import com.predykt.accounting.domain.entity.BankTransaction;
import com.predykt.accounting.dto.response.TransactionResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransactionMapper {
    
    TransactionResponse toResponse(BankTransaction transaction);
}