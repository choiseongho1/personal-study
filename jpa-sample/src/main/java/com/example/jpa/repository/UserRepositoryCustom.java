package com.example.jpa.repository;

import com.example.jpa.dto.UserCustomDto;
import com.example.jpa.dto.UserSearchDto;
import java.util.List;

public interface UserRepositoryCustom {
    List<UserCustomDto> findAllUserDtos();
    List<UserCustomDto> findAllUserDtosNestedDtoType1();
    List<UserCustomDto> findAllUserDtosNestedDtoType2();

    List<UserSearchDto> searchUsers(String name,  String email);
}
