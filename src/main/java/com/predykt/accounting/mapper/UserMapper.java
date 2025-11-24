package com.predykt.accounting.mapper;

import com.predykt.accounting.domain.entity.Role;
import com.predykt.accounting.domain.entity.User;
import com.predykt.accounting.dto.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {
    
    @Mapping(target = "fullName", expression = "java(user.getFullName())")
    @Mapping(target = "roles", expression = "java(mapRoles(user))")
    UserResponse toResponse(User user);
    
    default List<String> mapRoles(User user) {
        return user.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toList());
    }
}