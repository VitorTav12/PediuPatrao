package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.Log;
import org.springframework.data.repository.Repository;

public interface LogRepository extends Repository<Log, String> {

    <S extends Log> S save(S registro);
}