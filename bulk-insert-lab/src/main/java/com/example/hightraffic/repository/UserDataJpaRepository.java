package com.example.hightraffic.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.hightraffic.entity.UserData;

@Repository
public interface UserDataJpaRepository extends JpaRepository<UserData, Long> {
}
