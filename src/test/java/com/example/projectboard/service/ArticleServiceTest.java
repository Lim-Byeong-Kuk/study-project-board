package com.example.projectboard.service;

import com.example.projectboard.domain.Article;
import com.example.projectboard.domain.ArticleComment;
import com.example.projectboard.domain.Hashtag;
import com.example.projectboard.domain.UserAccount;
import com.example.projectboard.domain.constant.SearchType;
import com.example.projectboard.dto.*;
import com.example.projectboard.repository.ArticleRepository;
import com.example.projectboard.repository.HashtagRepository;
import com.example.projectboard.repository.UserAccountRepository;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * 테스트는 가볍게 만들수록 좋다.
 * Spring Boot 애플리케이션 띄우는 과정이 생기지 않게끔 테스트를 작성해본다.
 * 모킹을 이용하여 Dependency 를 추가한다.
 * 많이 사용되는 라이브러리 Mockito
 */
@DisplayName("비즈니스 로직 - 게시글")
@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @InjectMocks    // Mock 을 주입하는 대상을 @InjectMocks
    private ArticleService sut; // System Under Test : 테스트 대상 이라는 뜻
    @Mock
    private HashtagService hashtagService;
    @Mock           // 그 외 나머지 Mock 은 @Mock
    private ArticleRepository articleRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private HashtagRepository hashtagRepository;

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
        BDDMockito.given(articleRepository.findByTitleContaining(searchKeyword, pageable)).willReturn(Page.empty());

        // when
        Page<ArticleDto> articles = sut.searchArticles(searchType, searchKeyword, pageable);

        // then
        assertThat(articles).isEmpty();
        BDDMockito.then(articleRepository).should().findByTitleContaining(searchKeyword, pageable);
    }

    @DisplayName("검색어 없이 게시글을 해시태그 검색하면, 빈 페이지를 반환한다.")
    @Test
    void givenNoSearchParameters_whenSearchingArticlesViaHashtag_thenReturnsEmptyPage() {
        // given
        Pageable pageable = Pageable.ofSize(20);

        // when
        Page<ArticleDto> articles = sut.searchArticlesViaHashtag(null, pageable);

        // then
        assertThat(articles).isEqualTo(Page.empty(pageable));
        BDDMockito.then(hashtagRepository).shouldHaveNoInteractions();
        BDDMockito.then(articleRepository).shouldHaveNoInteractions();
    }

    @DisplayName("없는 해시태그를 검색하면, 빈 페이지를 반환한다.")
    @Test
    void givenNonexistentHashtag_whenSearchingArticlesViaHashtag_thenReturnsEmptyPage() {
        // Given
        String hashtagName = "난 없지롱";
        Pageable pageable = Pageable.ofSize(20);
        BDDMockito.given(articleRepository.findByHashtagNames(List.of(hashtagName), pageable)).willReturn(new PageImpl<>(List.of(), pageable, 0));

        // When
        Page<ArticleDto> articles = sut.searchArticlesViaHashtag(hashtagName, pageable);

        // Then
        assertThat(articles).isEqualTo(Page.empty(pageable));
        BDDMockito.then(articleRepository).should().findByHashtagNames(List.of(hashtagName), pageable);
    }

    @DisplayName("게시글을 해시태그 검색하면, 게시글 페이지를 반환한다.")
    @Test
    void givenHashtag_whenSearchingArticlesViaHashtag_thenReturnsArticlesPage() {
        // given
        String hashtagName = "java";
        Pageable pageable = Pageable.ofSize(20);
        Article expectedArticle = createArticle();
        BDDMockito.given(articleRepository.findByHashtagNames(List.of(hashtagName), pageable))
                .willReturn(new PageImpl<>(List.of(expectedArticle), pageable, 1));

        // when
        Page<ArticleDto> articles = sut.searchArticlesViaHashtag(hashtagName, pageable);

        // then
        assertThat(articles).isEqualTo(new PageImpl<>(List.of(ArticleDto.from(expectedArticle)), pageable, 1));
        BDDMockito.then(articleRepository).should().findByHashtagNames(List.of(hashtagName), pageable);
    }

    @DisplayName("게시글 ID로 조회하면, 댓글 달긴 게시글을 반환한다.")
    @Test
    void givenArticleId_whenSearchingArticleWithComments_thenReturnsArticleWithComments() {
        // viven
        Long articleId = 1L;
        Article article = createArticle();
        BDDMockito.given(articleRepository.findById(articleId)).willReturn(Optional.of(article));

        // when
        ArticleWithCommentsDto dto = sut.getArticleWithComments(articleId);

        // then
        assertThat(dto)
                .hasFieldOrPropertyWithValue("title", article.getTitle())
                .hasFieldOrPropertyWithValue("content", article.getContent())
                .hasFieldOrPropertyWithValue("hashtagDtos", article.getHashtags().stream()
                        .map(HashtagDto::from)
                        .collect(Collectors.toUnmodifiableSet())
                );
        BDDMockito.then(articleRepository).should().findById(articleId);
    }

    @DisplayName("댓글 달린 게시글이 없으면, 예외를 던진다.")
    @Test
    void givenNonexistentArticleId_whenSearchingArticleWithComments_thenThrowsException() {
        // given
        Long articleId = 0L;
        BDDMockito.given(articleRepository.findById(articleId)).willReturn(Optional.empty());

        // when
        Throwable t = catchThrowable(() -> sut.getArticleWithComments(articleId));

        // then
        assertThat(t)
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("게시글이 없습니다 - articleId: " + articleId);
        BDDMockito.then(articleRepository).should().findById(articleId);
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
        ArticleDto dto = sut.getArticle(articleId);

        // then
        assertThat(dto)
                .hasFieldOrPropertyWithValue("title", article.getTitle())
                .hasFieldOrPropertyWithValue("content", article.getContent())
                .hasFieldOrPropertyWithValue("hashtagDtos", article.getHashtags().stream()
                        .map(HashtagDto::from)
                        .collect(Collectors.toUnmodifiableSet())
                );
        BDDMockito.then(articleRepository).should().findById(articleId);
    }

    @DisplayName("게시글이 없으면, 예외를 던진다.")
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

    @DisplayName("게시글을 정보를 입력하면, 본문에서 해시태그 정보를 추출하여 해시태그 정보가 포함된 게시글을 생성한다.")
    @Test
    void givenArticleInfo_whenSavingArticle_thenExtractsHashtagsFromContentAndSavesArticleWithExtractedHashtags() {
        // any() 만 쓰면 아무거나 가능, any(Type.class) 는 Type 만 가능
        // 코드에 명시적으로 어떤일이 일어날 것이라는 것을 표현해 주는것, (작동하는 코드가 아니다. 있느나마나 함)
        // given
        ArticleDto dto = createArticleDto();
        Set<String> expectedHashtagNames = Set.of("java", "spring");
        Set<Hashtag> expectedHashtags = new HashSet<>();
        expectedHashtags.add(createHashtag("java"));

        BDDMockito.given(userAccountRepository.getReferenceById(dto.userAccountDto().userId())).willReturn(createUserAccount());
        BDDMockito.given(hashtagService.parseHashtagNames(dto.content())).willReturn(expectedHashtagNames);
        BDDMockito.given(hashtagService.findHashtagsByNames(expectedHashtagNames)).willReturn(expectedHashtags);
        BDDMockito.given(articleRepository.save(ArgumentMatchers.any(Article.class))).willReturn(createArticle());

        // when
        sut.saveArticle(dto);

        // then
        BDDMockito.then(userAccountRepository).should().getReferenceById(dto.userAccountDto().userId());
        BDDMockito.then(hashtagService).should().parseHashtagNames(dto.content());
        BDDMockito.then(hashtagService).should().findHashtagsByNames(expectedHashtagNames);
        BDDMockito.then(articleRepository).should().save(ArgumentMatchers.any(Article.class));
    }

    @DisplayName("게시글의 수정 정보를 입력하면, 게시글을 수정한다.")
    @Test
    void givenModifiedArticleInfo_whenUpdatingArticle_thenUpdatesArticle() {
        // given
        Article article = createArticle();
        ArticleDto dto = createArticleDto("새 타이틀", "새 내용 #springboot");
        Set<String> expectedHashtagNames = Set.of("springboot");
        Set<Hashtag> expectedHashtags = new HashSet<>();

        BDDMockito.given(articleRepository.getReferenceById(dto.id())).willReturn(article);
        BDDMockito.given(userAccountRepository.getReferenceById(dto.userAccountDto().userId())).willReturn(dto.userAccountDto().toEntity());
        BDDMockito.willDoNothing().given(articleRepository).flush();
        BDDMockito.willDoNothing().given(hashtagService).deleteHashtagWithoutArticles(ArgumentMatchers.any());
        BDDMockito.given(hashtagService.parseHashtagNames(dto.content())).willReturn(expectedHashtagNames);
        BDDMockito.given(hashtagService.findHashtagsByNames(expectedHashtagNames)).willReturn(expectedHashtags);

        // when
        sut.updateArticle(dto.id(), dto);

        // then
        assertThat(article)
                .hasFieldOrPropertyWithValue("title", dto.title())
                .hasFieldOrPropertyWithValue("content", dto.content())
                .extracting("hashtags", Assertions.as(InstanceOfAssertFactories.COLLECTION))
                    .hasSize(1)
                    .extracting("hashtagName")
                        .containsExactly("springboot");

        BDDMockito.then(articleRepository).should().getReferenceById(dto.id());
        BDDMockito.then(userAccountRepository).should().getReferenceById(dto.userAccountDto().userId());
        BDDMockito.then(articleRepository).should().flush();
        BDDMockito.then(hashtagService).should(Mockito.times(2)).deleteHashtagWithoutArticles(ArgumentMatchers.any());
        BDDMockito.then(hashtagService).should().parseHashtagNames(dto.content());
        BDDMockito.then(hashtagService).should().findHashtagsByNames(expectedHashtagNames);
    }

    @DisplayName("없는 게시글의 수정 정보를 입력하면, 경고 로그를 찍고 아무 것도 하지 않는다.")
    @Test
    void givenNonexistenArticleInfo_whenUpdatingArticle_thenLogsWarningAndDoesNothing() {
        /**
         *  getReferenceById 는 Id 가 존재하지 않을 경우 EntityNotFoundException 을 던진다.
         */
        // given
        ArticleDto dto = createArticleDto("새 타이틀", "새 내용");
        BDDMockito.given(articleRepository.getReferenceById(dto.id())).willThrow(EntityNotFoundException.class);

        // when
        sut.updateArticle(dto.id(), dto);

        // then
        BDDMockito.then(articleRepository).should().getReferenceById(dto.id());
        BDDMockito.then(userAccountRepository).shouldHaveNoInteractions();
        BDDMockito.then(hashtagService).shouldHaveNoInteractions();
    }

    @DisplayName("게시글 작성자가 아닌 사람이 수정 정보를 입력하면, 아무 것도 하지 않는다.")
    @Test
    void givenModifiedArticleInfoWithDifferentUser_whenUpdatingArticle_thenDoesNothing() {
        // Given
        Long differentArticleId = 22L;
        Article differentArticle = createArticle(differentArticleId);
        differentArticle.setUserAccount(createUserAccount("John"));
        ArticleDto dto = createArticleDto("새 타이틀", "새 내용");
        BDDMockito.given(articleRepository.getReferenceById(differentArticleId)).willReturn(differentArticle);
        BDDMockito.given(userAccountRepository.getReferenceById(dto.userAccountDto().userId())).willReturn(dto.userAccountDto().toEntity());

        // When
        sut.updateArticle(differentArticleId, dto);

        // Then
        BDDMockito.then(articleRepository).should().getReferenceById(differentArticleId);
        BDDMockito.then(userAccountRepository).should().getReferenceById(dto.userAccountDto().userId());
        BDDMockito.then(hashtagService).shouldHaveNoInteractions();
    }

    @DisplayName("게시글의 ID를 입력하면, 게시글을 삭제한다.")
    @Test
    void givenArticleId_whenDeletingArticle_thenDeletesArticle() {

        // given
        // delete가 반환 값이 없기 때문에 willDoNothing() 을 사용
        Long articleId = 1L;
        String userId = "lbk";
        BDDMockito.given(articleRepository.getReferenceById(articleId)).willReturn(createArticle());
        BDDMockito.willDoNothing().given(articleRepository).deleteByIdAndUserAccount_UserId(articleId, userId);
        BDDMockito.willDoNothing().given(articleRepository).flush();
        BDDMockito.willDoNothing().given(hashtagService).deleteHashtagWithoutArticles(ArgumentMatchers.any());

        // when
        sut.deleteArticle(1L, userId);

        // then
        BDDMockito.then(articleRepository).should().getReferenceById(articleId);
        BDDMockito.then(articleRepository).should().deleteByIdAndUserAccount_UserId(articleId, userId);
        BDDMockito.then(articleRepository).should().flush();
        BDDMockito.then(hashtagService).should(Mockito.times(2)).deleteHashtagWithoutArticles(ArgumentMatchers.any());
    }

    @DisplayName("게시글 수를 조회하면, 게시글 수를 반환한다.")
    @Test
    void givenNothing_whenCountingArticles_thenReturnsArticleCount() {
        // given
        long expected = 0L;
        BDDMockito.given(articleRepository.count()).willReturn(expected);

        // when
        long actual = sut.getArticleCount();

        // then
        assertThat(actual).isEqualTo(expected);
        BDDMockito.then(articleRepository).should().count();
    }

    @DisplayName("해시태그를 조회하면, 유니크 해시태그 리스트를 반환한다.")
    @Test
    void givenNothing_whenCalling_thenReturnsHashtags() {
        // given
        Article article = createArticle();
        List<String> expectedHashtags = List.of("java", "spring", "boot");
        BDDMockito.given(hashtagRepository.findAllHashtagNames()).willReturn(expectedHashtags);

        // when
        List<String> actualHashtags = sut.getHashtags();

        // then
        assertThat(actualHashtags).isEqualTo(expectedHashtags);
        BDDMockito.then(hashtagRepository).should().findAllHashtagNames();
    }

    private UserAccount createUserAccount() {
        return createUserAccount("lbk");
    }

    private UserAccount createUserAccount(String userId) {
        return UserAccount.of(
                userId,
                "password",
                "lbk@gmail.com",
                "Lbk",
                null
        );
    }

    private Article createArticle() {
        return createArticle(1L);
    }

    private Article createArticle(Long id) {
        Article article = Article.of(
                createUserAccount(),
                "title",
                "content"
        );

        article.addHashtags(Set.of(
                createHashtag(1L, "java"),
                createHashtag(2L, "spring")
        ));

        return article;
    }

    private Hashtag createHashtag(String hashtagName) {
        return createHashtag(1L, hashtagName);
    }

    private Hashtag createHashtag(Long id, String hashtagName) {
        Hashtag hashtag = Hashtag.of(hashtagName);
        ReflectionTestUtils.setField(hashtag, "id", id);

        return hashtag;
    }

    private HashtagDto createHashtagDto() {
        return HashtagDto.of("java");
    }

    private ArticleDto createArticleDto() {
        return createArticleDto("title", "content");
    }

    private ArticleDto createArticleDto(String title, String content) {
        return ArticleDto.of(
                1L,
                createUserAccountDto(),
                title,
                content,
                null,
                LocalDateTime.now(),
                "Lbk",
                LocalDateTime.now(),
                "Lbk");
    }

    private UserAccountDto createUserAccountDto() {
        return UserAccountDto.of(
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

}
