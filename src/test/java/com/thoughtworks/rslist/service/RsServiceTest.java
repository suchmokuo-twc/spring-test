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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RsServiceTest {

    RsService rsService;

    @Mock
    RsEventRepository rsEventRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    VoteRepository voteRepository;

    @Mock
    TradeRepository tradeRepository;

    LocalDateTime localDateTime;

    Vote vote;

    @BeforeEach
    void setUp() {
        initMocks(this);
        rsService = new RsService(rsEventRepository, userRepository, voteRepository, tradeRepository);
        localDateTime = LocalDateTime.now();
        vote = Vote.builder().voteNum(2).rsEventId(1).time(localDateTime).userId(1).build();
    }

    @Test
    void shouldVoteSuccess() {
        // given

        UserDto userDto =
                UserDto.builder()
                        .voteNum(5)
                        .phone("18888888888")
                        .gender("female")
                        .email("a@b.com")
                        .age(19)
                        .userName("xiaoli")
                        .id(2)
                        .build();
        RsEventDto rsEventDto =
                RsEventDto.builder()
                        .eventName("event name")
                        .id(1)
                        .keyword("keyword")
                        .voteNum(2)
                        .user(userDto)
                        .build();

        when(rsEventRepository.findById(anyInt())).thenReturn(Optional.of(rsEventDto));
        when(userRepository.findById(anyInt())).thenReturn(Optional.of(userDto));
        // when
        rsService.vote(vote, 1);
        // then
        verify(voteRepository)
                .save(
                        VoteDto.builder()
                                .num(2)
                                .localDateTime(localDateTime)
                                .user(userDto)
                                .rsEvent(rsEventDto)
                                .build());
        verify(userRepository).save(userDto);
        verify(rsEventRepository).save(rsEventDto);
    }

    @Test
    void shouldThrowExceptionWhenUserNotExist() {
        // given
        when(rsEventRepository.findById(anyInt())).thenReturn(Optional.empty());
        when(userRepository.findById(anyInt())).thenReturn(Optional.empty());
        //when&then
        assertThrows(
                RuntimeException.class,
                () -> {
                    rsService.vote(vote, 1);
                });
    }

    @Test
    void shouldBuySuccess() {
        int rankToBuy = 1;
        int currentAmount = 100;
        int amount = 101;
        int rsEventId = 1;

        when(tradeRepository.findCurrentAmountByRank(rankToBuy)).thenReturn(Optional.of(currentAmount));

        rsService.buy(Trade.builder()
                .amount(amount)
                .rank(rankToBuy)
                .build(), rsEventId);

        verify(tradeRepository).save(TradeDto.builder()
                .rank(rankToBuy)
                .amount(amount)
                .rsEventDto(RsEventDto.builder()
                        .id(rsEventId)
                        .build())
                .build());
    }

    @Test
    void shouldBuyFailWhenNoEnoughAmount() {
        int rankToBuy = 1;
        int currentAmount = 100;
        int amount = 90;
        int rsEventId = 1;

        when(tradeRepository.findCurrentAmountByRank(rankToBuy)).thenReturn(Optional.of(currentAmount));

        assertThrows(AmountNotEnoughException.class, () -> {
            rsService.buy(Trade.builder()
                    .amount(amount)
                    .rank(rankToBuy)
                    .build(), rsEventId);
        });
    }

    @Test
    void shouldGetSortedRsEventList() {
        when(tradeRepository.findAll()).thenReturn(Arrays.asList(
                TradeDto.builder()
                        .rank(1)
                        .amount(100)
                        .rsEventDto(RsEventDto.builder()
                                .id(1)
                                .build())
                        .build(),

                TradeDto.builder()
                        .rank(1)
                        .amount(120)
                        .rsEventDto(RsEventDto.builder()
                                .id(2)
                                .build())
                        .build(),

                TradeDto.builder()
                        .rank(3)
                        .amount(100)
                        .rsEventDto(RsEventDto.builder()
                                .id(3)
                                .build())
                        .build()
        ));

        when(rsEventRepository.findAll()).thenReturn(Arrays.asList(
                RsEventDto.builder()
                        .id(1)
                        .voteNum(1)
                        .build(),

                RsEventDto.builder()
                        .id(2)
                        .voteNum(2)
                        .build(),

                RsEventDto.builder()
                        .id(3)
                        .voteNum(3)
                        .build(),

                RsEventDto.builder()
                        .id(4)
                        .voteNum(4)
                        .build()
        ));

        List<RsEvent> allRsEvents = rsService.getAllRsEvents();

        assertIterableEquals(
                allRsEvents.stream().map(RsEvent::getId).collect(Collectors.toList()),
                Arrays.asList(2, 4, 3, 1)
        );
    }
}
