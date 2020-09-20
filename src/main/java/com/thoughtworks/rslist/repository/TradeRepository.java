package com.thoughtworks.rslist.repository;

import com.thoughtworks.rslist.dto.TradeDto;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface TradeRepository extends CrudRepository<TradeDto, Integer> {

    List<TradeDto> findAll();

    @Query("SELECT MAX(t.amount) FROM TradeDto t WHERE t.rank = :rank")
    Optional<Integer> findCurrentAmountByRank(int rank);
}
