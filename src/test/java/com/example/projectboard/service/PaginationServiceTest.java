package com.example.projectboard.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

@DisplayName("비즈니스 로직 - 페이지네이션")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = PaginationService.class)  // SpringBootTest 의 무게를 줄이는 방법
class PaginationServiceTest {

    private final PaginationService sut;

    PaginationServiceTest(@Autowired PaginationService paginationService) {
        this.sut = paginationService;
    }


    @DisplayName("현재 페이지 번호와 총 페이지 수를 주면, 페이징 바 리스트를 만들어준다.")
    @MethodSource
    @ParameterizedTest(name = "[{index}] 현재 페이지: {0}, 총 페이지: {1} => {2}")
        // 값을 여러번 연속적으로 주입해서 동일한 메소드를 여러번 테스트한는 기능
    void givenCurrentPageNumberAndTotalPages_whenCalculating_thenReturnsPaginationBarNumbers(int currentPageNumber, int totalPages, List<Integer> expected) {
        // given

        // when
        List<Integer> actual = sut.getPaginationBarNumbers(currentPageNumber, totalPages);

        // then
        assertThat(actual).isEqualTo(expected);
    }

    /**
     * 입력값을 넣어주는 메소드 소스는 메소드 형식으로 만든다.
     * static 으로 만들어야 한다.
     * 테스트 이름과 동일한 이름으로 만들거나 @MethodSource(메소드이름) 이런식으로 설정해 줘야 한다.
     */
    static Stream<Arguments> givenCurrentPageNumberAndTotalPages_whenCalculating_thenReturnsPaginationBarNumbers() {
        return Stream.of( // 이 안에 검증해 보고 싶은 값들을 나열한다.
                Arguments.arguments(0, 13, List.of(0, 1, 2, 3, 4)),  // 123 개 글을 10개씩 묶으면 13페이지 나옴
                Arguments.arguments(1, 13, List.of(0, 1, 2, 3, 4)),  // Page 인터페이스에서 0부터 시작하므로 0부터 시작, 뷰에서 더하기 1을 하자
                Arguments.arguments(2, 13, List.of(0, 1, 2, 3, 4)),  // List.of 안에 숫자들을 페이지 바에서 보여줄 페이지 숫자이다.
                Arguments.arguments(3, 13, List.of(1, 2, 3, 4, 5)),
                Arguments.arguments(4, 13, List.of(2, 3, 4, 5, 6)),
                Arguments.arguments(5, 13, List.of(3, 4, 5, 6, 7)),
                Arguments.arguments(6, 13, List.of(4, 5, 6, 7, 8)),
                Arguments.arguments(10, 13, List.of(8, 9, 10, 11, 12)),
                Arguments.arguments(11, 13, List.of(9, 10, 11, 12)),
                Arguments.arguments(12, 13, List.of(10, 11, 12))
        );
    }

    @DisplayName("현재 설정되어 있는 페이지네이션 바의 길이를 알려준다.")
    @Test       // 스펙의 명세를 코드에 드러내기 위한 테스트, 협업자들이 알 수 있도록 도와준다.
    void givenNothing_whenCalling_thenReturnsCurrentBarLength() {
        // given & when
        int barLength = sut.currentBarLength();

        // then
        assertThat(barLength).isEqualTo(5);
     }

}
