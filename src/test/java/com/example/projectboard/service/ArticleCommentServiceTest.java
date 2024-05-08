package com.example.projectboard.service;

import com.example.projectboard.domain.Article;
import com.example.projectboard.domain.ArticleComment;
import com.example.projectboard.domain.Hashtag;
import com.example.projectboard.domain.UserAccount;
import com.example.projectboard.dto.ArticleCommentDto;
import com.example.projectboard.dto.UserAccountDto;
import com.example.projectboard.repository.ArticleCommentRepository;
import com.example.projectboard.repository.ArticleRepository;
import com.example.projectboard.repository.UserAccountRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DisplayName("비즈니스 로직 - 댓글")
@ExtendWith(MockitoExtension.class)
class ArticleCommentServiceTest {

    @InjectMocks
    private ArticleCommentService sut;
    @Mock
    private ArticleRepository articleRepository;
    @Mock
    private ArticleCommentRepository articleCommentRepository;
    @Mock
    private UserAccountRepository userAccountRepository;

    @DisplayName("게시글 ID로 조회하면, 해당하는 댓글 리스트를 반환한다.")
    @Test
    void givenArticleId_whenSearchingComments_thenReturnsArticleComments() {
        // given
        Long articleId = 1L;

        ArticleComment expectedParentComment = createArticleComment(1L, "parent content");
        ArticleComment expectedChildComment = createArticleComment(2L, "child content");
        expectedChildComment.setParentCommentId(expectedParentComment.getId());
        BDDMockito.given(articleCommentRepository.findByArticle_Id(articleId)).willReturn(List.of(
                expectedParentComment,
                expectedChildComment
        ));

        // when
        List<ArticleCommentDto> actual = sut.searchArticleComments(articleId);

        // then
        assertThat(actual).hasSize(2);
        assertThat(actual)
                .extracting("id", "articleId", "parentCommentId", "content")
                .containsExactlyInAnyOrder(
                        tuple(1L, 1L, null, "parent content"),
                        tuple(2L, 1L, 1L, "child content")
                );

        BDDMockito.then(articleCommentRepository).should().findByArticle_Id(articleId);
    }

    @DisplayName("댓글 정보를 입력하면, 댓글을 저장한다.")
    @Test
    void givenArticleCommentInfo_whenSavingArticleComment_thenSavesArticleComment() {
        // given
        ArticleCommentDto dto = createArticleCommentDto("댓글");
        BDDMockito.given(articleRepository.getReferenceById(dto.articleId())).willReturn(createArticle());
        BDDMockito.given(userAccountRepository.getReferenceById(dto.userAccountDto().userId())).willReturn(createUserAccount());
        BDDMockito.given(articleCommentRepository.save(ArgumentMatchers.any(ArticleComment.class))).willReturn(null);

        // when
        sut.saveArticleComment(dto);

        // then
        BDDMockito.then(articleRepository).should().getReferenceById(dto.articleId());
        BDDMockito.then(userAccountRepository).should().getReferenceById(dto.userAccountDto().userId());
        BDDMockito.then(articleCommentRepository).should(Mockito.never()).getReferenceById(ArgumentMatchers.anyLong());
        BDDMockito.then(articleCommentRepository).should().save(ArgumentMatchers.any(ArticleComment.class));
    }

    @DisplayName("댓글 저장을 시도했는데 맞는 게시글이 없으면, 경고 로그를 찍고 아무것도 안 한다.")
    @Test
    void givenNonexistenArticle_whenSavingArticleComment_thenLogsSituationAndDoesNothing() {
        // given
        ArticleCommentDto dto = createArticleCommentDto("댓글");
        BDDMockito.given(articleRepository.getReferenceById(dto.articleId())).willThrow(EntityNotFoundException.class);

        // when
        sut.saveArticleComment(dto);

        // then
        BDDMockito.then(articleRepository).should().getReferenceById(dto.articleId());
        BDDMockito.then(userAccountRepository).shouldHaveNoInteractions();
        BDDMockito.then(articleCommentRepository).shouldHaveNoInteractions();
    }

    @Disabled
    @DisplayName("댓글 정보를 입력하면, 댓글을 수정한다.")
    @Test
    void givenArticleCommentInfo_whenUpdatingArticleComment_thenUpdatesArticleComment() {
        // given
        String oldcontent = "content";
        String updateContent = "댓글";
        ArticleComment articleComment = createArticleComment(1L, oldcontent);
        ArticleCommentDto dto = createArticleCommentDto(updateContent);
        BDDMockito.given(articleCommentRepository.getReferenceById(dto.id())).willReturn(articleComment);

        // when
        sut.updateArticleComment(dto);

        // then
        assertThat(articleComment.getContent())
                .isNotEqualTo(oldcontent)
                .isEqualTo(updateContent);
        BDDMockito.then(articleCommentRepository).should().getReferenceById(dto.id());
    }

    @DisplayName("없는 댓글 정보를 수정하려고 하면, 경고 로그를 찍고 아무 것도 안 한다.")
    @Test
    void givenNoexistenArticleComment_whenUpdatingArticleComment_thenLogsWarningAndDoesNothing() {
        // given
        ArticleCommentDto dto = createArticleCommentDto("댓글");
        BDDMockito.given(articleCommentRepository.getReferenceById(dto.id())).willThrow(EntityNotFoundException.class);

        // when
        sut.updateArticleComment(dto);

        // then
        BDDMockito.then(articleCommentRepository).should().getReferenceById(dto.id());
    }

    @DisplayName("부모 댓글 ID와 댓글 정보를 입력하면, 대댓글을 저장한다.")
    @Test
    void givenParentCommentIdAndArticleCommentInfo_whenSaving_thenSavesChildComment() {
        // Given
        Long parentCommentId = 1L;
        ArticleComment parent = createArticleComment(parentCommentId, "댓글");
        ArticleCommentDto child = createArticleCommentDto(parentCommentId, "대댓글");
        BDDMockito.given(articleRepository.getReferenceById(child.articleId())).willReturn(createArticle());
        BDDMockito.given(userAccountRepository.getReferenceById(child.userAccountDto().userId())).willReturn(createUserAccount());
        BDDMockito.given(articleCommentRepository.getReferenceById(child.parentCommentId())).willReturn(parent);

        // When
        sut.saveArticleComment(child);

        // Then
        assertThat(child.parentCommentId()).isNotNull();
        BDDMockito.then(articleRepository).should().getReferenceById(child.articleId());
        BDDMockito.then(userAccountRepository).should().getReferenceById(child.userAccountDto().userId());
        BDDMockito.then(articleCommentRepository).should().getReferenceById(child.parentCommentId());
        BDDMockito.then(articleCommentRepository).should(Mockito.never()).save(ArgumentMatchers.any(ArticleComment.class));
    }

    @DisplayName("댓글 ID를 입력하면, 댓글을 삭제한다.")
    @Test
    void givenArticleCommentId_whenDeletingArticleComment_thenDeletesArticleComment() {
        // given
        Long articleCommentId = 1L;
        String userId = "lbk";
        BDDMockito.willDoNothing().given(articleCommentRepository).deleteByIdAndUserAccount_UserId(articleCommentId, userId);

        // when
        sut.deleteArticleComment(articleCommentId, userId);

        // then
        BDDMockito.then(articleCommentRepository).should().deleteByIdAndUserAccount_UserId(articleCommentId, userId);
    }

    private ArticleCommentDto createArticleCommentDto(String content) {
        return createArticleCommentDto(null, content);
    }

    private ArticleCommentDto createArticleCommentDto(Long parentCommentId, String content) {
        return createArticleCommentDto(1L, parentCommentId, content);
    }

    private ArticleCommentDto createArticleCommentDto(Long id, Long parentCommentId, String content) {
        return ArticleCommentDto.of(
                id,
                1L,
                createUserAccountDto(),
                parentCommentId,
                content,
                LocalDateTime.now(),
                "lbk",
                LocalDateTime.now(),
                "lbk"
        );
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

    private ArticleComment createArticleComment(Long id, String content) {
        ArticleComment articleComment = ArticleComment.of(
                createArticle(),
                createUserAccount(),
                content
        );
        ReflectionTestUtils.setField(articleComment, "id", id);

        return articleComment;
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
        Article article = Article.of(
                createUserAccount(),
                "title",
                "content"
        );
        ReflectionTestUtils.setField(article, "id", 1L);
        article.addHashtags(Set.of(createHashtag(article)));

        return article;
    }

    private Hashtag createHashtag(Article article) {
        return Hashtag.of("java");
    }

}
