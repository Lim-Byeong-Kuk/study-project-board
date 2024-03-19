package com.example.projectboard.service;

import com.example.projectboard.domain.Article;
import com.example.projectboard.domain.ArticleComment;
import com.example.projectboard.domain.UserAccount;
import com.example.projectboard.domain.type.SearchType;
import com.example.projectboard.dto.ArticleCommentDto;
import com.example.projectboard.dto.ArticleDto;
import com.example.projectboard.dto.ArticleWithCommentsDto;
import com.example.projectboard.dto.UserAccountDto;
import com.example.projectboard.repository.ArticleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 *  테스트는 가볍게 만들수록 좋다.
 *  Spring Boot 애플리케이션 띄우는 과정이 생기지 않게끔 테스트를 작성해본다.
 *  모킹을 이용하여 Dependency 를 추가한다.
 *  많이 사용되는 라이브러리 Mockito
 */
@DisplayName("비즈니스 로직 - 게시글")
@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @InjectMocks    // Mock 을 주입하는 대상을 @InjectMocks
    private ArticleService sut; // System Under Test : 테스트 대상 이라는 뜻
    @Mock           // 그 외 나머지 Mock 은 @Mock
    private ArticleRepository articleRepository;

    @DisplayName("검색어 없이 게시글을 검색하면, 게시글 페이지를 반환한다.")
    @Test
    void givenNoSearchParameters_whenSearchingArticles_thenReturnsArticlePage() {
        /**
         *  검색
         *  페이지네이션
         *  정렬 기능
         */
        // given
        Pageable pageable = Pageable.ofSize(20);
        BDDMockito.given(articleRepository.findAll(pageable)).willReturn(Page.empty());

        // when
        Page<ArticleDto> articles = sut.searchArticles(null, null, pageable);

        // then
        assertThat(articles).isEmpty();
        BDDMockito.then(articleRepository).should().findAll(pageable);
    }

    @DisplayName("검색어와 함께 게시글을 검색하면, 게시글 페이지를 반환한다.")
    @Test
    void givenSearchParameters_whenSearchingArticles_thenReturnsArticlePage() {
        // given
        SearchType searchType = SearchType.TITLE;
        String searchKeyword = "title";
        Pageable pageable = Pageable.ofSize(20);
        BDDMockito.given(articleRepository.findByTitle(searchKeyword, pageable)).willReturn(Page.empty());

        // when
        Page<ArticleDto> articles = sut.searchArticles(searchType, searchKeyword, pageable);

        // then
        assertThat(articles).isEmpty();
        BDDMockito.then(articleRepository).should().findByTitle(searchKeyword, pageable);
    }

    @DisplayName("게시글을 조회하면, 게시글을 반환한다.")
    @Test
    void givenArticleId_whenSearchingArticle_thenReturnsArticle() {
        /**
         *  각 게시글 페이지로 이동
         */
        // given
        Long articleId = 1L;
        Article article = createArticle();
        BDDMockito.given(articleRepository.findById(articleId)).willReturn(Optional.of(article));

        // when
        ArticleWithCommentsDto dto = sut.getArticle(articleId);

        // then
        assertThat(dto)
                .hasFieldOrPropertyWithValue("title", article.getTitle())
                .hasFieldOrPropertyWithValue("content", article.getContent())
                .hasFieldOrPropertyWithValue("hashtag", article.getHashtag());
        BDDMockito.then(articleRepository).should().findById(articleId);
    }

    @DisplayName("없는 게시글을 조회하면, 예외를 던진다.")
    @Test
    void givenNonexistenArticleId_whenSearchingArticle_thenThrowsException() {
        // given
        Long articleId = 0L;
        BDDMockito.given(articleRepository.findById(articleId)).willReturn(Optional.empty());

        // when
        Throwable t = catchThrowable(() -> sut.getArticle(articleId));

        // then
        assertThat(t)
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("게시글이 없습니다 - articleId: " + articleId);
        BDDMockito.then(articleRepository).should().findById(articleId);
    }

    @DisplayName("게시글을 정보를 입력하면, 게시글을 생성한다.")
    @Test
    void givenArticleInfo_whenSavingArticle_thenSavesArticle() {
        /**
         *  유닛테스트
         */
        // any() 만 쓰면 아무거나 가능, any(Type.class) 는 Type 만 가능
        // 코드에 명시적으로 어떤일이 일어날 것이라는 것을 표현해 주는것, (작동하는 코드가 아니다. 있느나마나 함)
        // given
        ArticleDto dto = createArticleDto();
        BDDMockito.given(articleRepository.save(ArgumentMatchers.any(Article.class))).willReturn(createArticle());

        // when
        sut.saveArticle(dto);

        // then
        BDDMockito.then(articleRepository).should().save(ArgumentMatchers.any(Article.class));
    }

    @DisplayName("게시글의 수정 정보를 입력하면, 게시글을 수정한다.")
    @Test
    void givenModifiedInfo_whenUpdatingArticle_thenUpdatesArticle() {
        // given
        Article article = createArticle();
        ArticleDto dto = createArticleDto("새 타이틀", "새 내용", "#springboot");
        BDDMockito.given(articleRepository.getReferenceById(dto.id())).willReturn(article);

        // when
        sut.updateArticle(dto);

        // then
        assertThat(article)
                .hasFieldOrPropertyWithValue("title", dto.title())
                .hasFieldOrPropertyWithValue("content", dto.content())
                .hasFieldOrPropertyWithValue("hashtag", dto.hashtag());
        BDDMockito.then(articleRepository).should().getReferenceById(dto.id());
    }

    @DisplayName("없는 게시글의 수정 정보를 입력하면, 경고 로그를 찍고 아무 것도 하지 않는다.")
    @Test
    void givenNonexistenArticleInfo_whenUpdatingArticle_thenLogsWarningAndDoesNothing() {
        // given
        ArticleDto dto = createArticleDto("새 타이틀", "새 내용", "#springboot");
        BDDMockito.given(articleRepository.getReferenceById(dto.id())).willThrow(EntityNotFoundException.class);

        // when
        sut.updateArticle(dto);

        // then
        BDDMockito.then(articleRepository).should().getReferenceById(dto.id());
    }

    @DisplayName("게시글의 ID를 입력하면, 게시글을 삭제한다.")
    @Test
    void givenArticleId_whenDeletingArticle_thenDeletesArticle() {

        // given
        // delete가 반환 값이 없기 때문에 willDoNothing() 을 사용
        Long articleId = 1L;
        BDDMockito.willDoNothing().given(articleRepository).deleteById(articleId);

        // when
        sut.deleteArticle(1L);

        // then
        BDDMockito.then(articleRepository).should().deleteById(articleId);
    }

    private UserAccount createUserAccount() {
        return UserAccount.of(
                "lbk",
                "password",
                "lbk@gmail.com",
                "lbk",
                null
        );
    }

    private Article createArticle() {
        return Article.of(
                createUserAccount(),
                "title",
                "content",
                "#Hashtag"
        );
    }

    private ArticleDto createArticleDto() {
        return createArticleDto("title", "content", "#java");
    }

    private ArticleDto createArticleDto(String title, String content, String hashtag) {
        return ArticleDto.of(1L,
                createUserAccountDto(),
                title,
                content,
                hashtag,
                LocalDateTime.now(),
                "Lbk",
                LocalDateTime.now(),
                "Lbk");
    }

    private ArticleCommentDto createArticleCommentDto(String content) {
        return ArticleCommentDto.of(
                1L,
                1L,
                createUserAccountDto(),
                content,
                LocalDateTime.now(),
                "lbk",
                LocalDateTime.now(),
                "lbk"
        );
    }

    private UserAccountDto createUserAccountDto() {
        return UserAccountDto.of(
                1L,
                "lbk",
                "password",
                "lbk@mail.com",
                "lbk",
                "this is memo",
                LocalDateTime.now(),
                "lbk",
                LocalDateTime.now(),
                "lbk"
        );
    }

    private ArticleComment createArticleComment(String content) {
        return ArticleComment.of(
                Article.of(createUserAccount(), "title", "content", "hashtag"),
                createUserAccount(),
                content
        );
    }

}
