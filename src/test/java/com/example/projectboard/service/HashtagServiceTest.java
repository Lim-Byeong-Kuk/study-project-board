package com.example.projectboard.service;

import com.example.projectboard.domain.Hashtag;
import com.example.projectboard.repository.HashtagRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;


@DisplayName("비지니스 로직 - 해시태그")
@ExtendWith(MockitoExtension.class)
class HashtagServiceTest {
    @InjectMocks
    private HashtagService sut;
    @Mock
    private HashtagRepository hashtagRepository;

    @DisplayName("본문을 파싱하면, 해시태그 이름들을 중복 없이 반환한다.")
    @MethodSource
    @ParameterizedTest(name = "[{index}] \"{0}\" => {1}")
    void givenContent_whenParsing_thenReturnsUniqueHashtagNames(String input, Set<String> expected) {
        // given

        // when
        Set<String> actual = sut.parseHashtagNames(input);

        // then
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
        BDDMockito.then(hashtagRepository).shouldHaveNoInteractions();
    }

    static Stream<Arguments> givenContent_whenParsing_thenReturnsUniqueHashtagNames() {
        return Stream.of(
                Arguments.arguments(null, Set.of()),
                Arguments.arguments("", Set.of()),
                Arguments.arguments("   ", Set.of()),
                Arguments.arguments("#", Set.of()),
                Arguments.arguments("  #", Set.of()),
                Arguments.arguments("#   ", Set.of()),
                Arguments.arguments("java", Set.of()),
                Arguments.arguments("java#", Set.of()),
                Arguments.arguments("ja#va", Set.of("va")),
                Arguments.arguments("#java", Set.of("java")),
                Arguments.arguments("#java_spring", Set.of("java_spring")),
                Arguments.arguments("#java-spring", Set.of("java")),
                Arguments.arguments("#_java_spring", Set.of("_java_spring")),
                Arguments.arguments("#-java-spring", Set.of()),
                Arguments.arguments("#_java_spring__", Set.of("_java_spring__")),
                Arguments.arguments("#java#spring", Set.of("java", "spring")),
                Arguments.arguments("#java #spring", Set.of("java", "spring")),
                Arguments.arguments("#java  #spring", Set.of("java", "spring")),
                Arguments.arguments("#java   #spring", Set.of("java", "spring")),
                Arguments.arguments("#java     #spring", Set.of("java", "spring")),
                Arguments.arguments("  #java     #spring ", Set.of("java", "spring")),
                Arguments.arguments("   #java     #spring   ", Set.of("java", "spring")),
                Arguments.arguments("#java#spring#부트", Set.of("java", "spring", "부트")),
                Arguments.arguments("#java #spring#부트", Set.of("java", "spring", "부트")),
                Arguments.arguments("#java#spring #부트", Set.of("java", "spring", "부트")),
                Arguments.arguments("#java,#spring,#부트", Set.of("java", "spring", "부트")),
                Arguments.arguments("#java.#spring;#부트", Set.of("java", "spring", "부트")),
                Arguments.arguments("#java|#spring:#부트", Set.of("java", "spring", "부트")),
                Arguments.arguments("#java #spring  #부트", Set.of("java", "spring", "부트")),
                Arguments.arguments("   #java,? #spring  ...  #부트 ", Set.of("java", "spring", "부트")),
                Arguments.arguments("#java#java#spring#부트", Set.of("java", "spring", "부트")),
                Arguments.arguments("#java#java#java#spring#부트", Set.of("java", "spring", "부트")),
                Arguments.arguments("#java#spring#java#부트#java", Set.of("java", "spring", "부트")),
                Arguments.arguments("#java#스프링 아주 긴 글~~~~~~~~~~~~~~~~~~~~~", Set.of("java", "스프링")),
                Arguments.arguments("아주 긴 글~~~~~~~~~~~~~~~~~~~~~#java#스프링", Set.of("java", "스프링")),
                Arguments.arguments("아주 긴 글~~~~~~#java#스프링~~~~~~~~~~~~~~~", Set.of("java", "스프링")),
                Arguments.arguments("아주 긴 글~~~~~~#java~~~~~~~#스프링~~~~~~~~", Set.of("java", "스프링"))
        );
    }

    @DisplayName("해시태그 이름들을 입력하면, 저장된 해시태그 중 이름에 매칭하는 것들을 중복 없이 반환한다.")
    @Test
    void givenHashtagNames_whenFindingHashtags_thenReturnsHashtagSet() {
        // given
        Set<String> hashtagNames = Set.of("java", "spring", "boots");
        BDDMockito.given(hashtagRepository.findByHashtagNameIn(hashtagNames)).willReturn(List.of(
                Hashtag.of("java"),
                Hashtag.of("spring")
        ));

        // when
        Set<Hashtag> hashtags = sut.findHashtagsByNames(hashtagNames);

        // then
        assertThat(hashtags).hasSize(2);
        BDDMockito.then(hashtagRepository).should().findByHashtagNameIn(hashtagNames);
    }

}
