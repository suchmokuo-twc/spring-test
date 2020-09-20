package com.thoughtworks.rslist.service;

import com.thoughtworks.rslist.domain.RsEvent;
import com.thoughtworks.rslist.domain.Trade;
import com.thoughtworks.rslist.domain.Vote;
import com.thoughtworks.rslist.dto.RsEventDto;
import com.thoughtworks.rslist.dto.TradeDto;
import com.thoughtworks.rslist.dto.UserDto;
import com.thoughtworks.rslist.dto.VoteDto;
import com.thoughtworks.rslist.exception.AmountNotEnoughException;
import com.thoughtworks.rslist.repository.RsEventRepository;
import com.thoughtworks.rslist.repository.TradeRepository;
import com.thoughtworks.rslist.repository.UserRepository;
import com.thoughtworks.rslist.repository.VoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RsService {

    final RsEventRepository rsEventRepository;
    final UserRepository userRepository;
    final VoteRepository voteRepository;
    final TradeRepository tradeRepository;

    @Autowired
    public RsService(RsEventRepository rsEventRepository,
                     UserRepository userRepository,
                     VoteRepository voteRepository,
                     TradeRepository tradeRepository) {
        this.rsEventRepository = rsEventRepository;
        this.userRepository = userRepository;
        this.voteRepository = voteRepository;
        this.tradeRepository = tradeRepository;
    }

    public void vote(Vote vote, int rsEventId) {
        Optional<RsEventDto> rsEventDto = rsEventRepository.findById(rsEventId);
        Optional<UserDto> userDto = userRepository.findById(vote.getUserId());
        if (!rsEventDto.isPresent()
                || !userDto.isPresent()
                || vote.getVoteNum() > userDto.get().getVoteNum()) {
            throw new RuntimeException();
        }
        VoteDto voteDto =
                VoteDto.builder()
                        .localDateTime(vote.getTime())
                        .num(vote.getVoteNum())
                        .rsEvent(rsEventDto.get())
                        .user(userDto.get())
                        .build();
        voteRepository.save(voteDto);
        UserDto user = userDto.get();
        user.setVoteNum(user.getVoteNum() - vote.getVoteNum());
        userRepository.save(user);
        RsEventDto rsEvent = rsEventDto.get();
        rsEvent.setVoteNum(rsEvent.getVoteNum() + vote.getVoteNum());
        rsEventRepository.save(rsEvent);
    }

    public void buy(Trade trade, int rsEventId) {
        Optional<Integer> currentAmountOptional = tradeRepository.findCurrentAmountByRank(trade.getRank());

        if (currentAmountOptional.isPresent()) {
            int currentAmount = currentAmountOptional.get();

            if (currentAmount >= trade.getAmount()) {
                throw new AmountNotEnoughException();
            }
        }

        tradeRepository.save(TradeDto.builder()
                .amount(trade.getAmount())
                .rank(trade.getRank())
                .rsEventDto(RsEventDto.builder()
                        .id(rsEventId)
                        .build())
                .build());
    }

    public List<RsEvent> getAllRsEvents() {
        List<RsEvent> rsEventList = rsEventRepository.findAll().stream()
                .map(item -> RsEvent.builder()
                        .id(item.getId())
                        .eventName(item.getEventName())
                        .keyword(item.getKeyword())
                        .userId(item.getId())
                        .voteNum(item.getVoteNum())
                        .build())
                .collect(Collectors.toList());

        return sortRsEventList(rsEventList);
    }

    private List<RsEvent> sortRsEventList(List<RsEvent> rsEventList) {
        rsEventList.sort((a, b) -> b.getVoteNum() - a.getVoteNum());

        Map<Integer, Integer> rankRsEventIdMap = getRankRsEventIdMap();

        int size = rsEventList.size();
        Iterator<RsEvent> rsEventIterator = rsEventList.iterator();
        List<RsEvent> result = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            int rank = i + 1;

            if (rankRsEventIdMap.containsKey(rank)) {
                int rsEventId = rankRsEventIdMap.get(rank);
                RsEvent rsEventFromList = getRsEventFromList(rsEventList, rsEventId);
                result.add(rsEventFromList);
            } else {
                RsEvent nextRsEvent = rsEventIterator.next();
                addRsEventIfNotExists(result, nextRsEvent);
            }
        }

        while (rsEventIterator.hasNext()) {
            RsEvent nextRsEvent = rsEventIterator.next();
            addRsEventIfNotExists(result, nextRsEvent);
        }

        return result;
    }

    private Map<Integer, Integer> getRankRsEventIdMap() {
        List<TradeDto> allTrades = tradeRepository.findAll();
        Map<Integer, TradeDto> rankMap = new HashMap<>();

        allTrades.forEach(tradeDto -> {
            int rank = tradeDto.getRank();
            int amount = tradeDto.getAmount();

            if (rankMap.containsKey(rank)) {
                int currentAmount = rankMap.get(rank).getAmount();

                if (currentAmount >= amount) {
                    return;
                }
            }

            rankMap.put(rank, tradeDto);
        });

        return rankMap.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getRsEventDto().getId()));
    }

    private RsEvent getRsEventFromList(List<RsEvent> rsEventList, int rsEventId) {
        return rsEventList
                .stream()
                .filter(item -> item.getId() == rsEventId)
                .findFirst()
                .get();
    }

    private boolean listNotContainsRsEvent(List<RsEvent> rsEventList, int rsEventId) {
        return rsEventList
                .stream()
                .noneMatch(item -> item.getId() == rsEventId);
    }

    private void addRsEventIfNotExists(List<RsEvent> rsEventList, RsEvent rsEvent) {
        if (listNotContainsRsEvent(rsEventList, rsEvent.getId())) {
            rsEventList.add(rsEvent);
        }
    }
}
