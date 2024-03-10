package com.example.projectboard.repository;

import com.example.projectboard.config.JpaConfig;
import com.example.projectboard.domain.Article;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JPA 연결 테스트")
@Import(JpaConfig.class)
@DataJpaTest
class JpaRepositoryTest {

    /**
     *  최신 버전 스프링부트는 테스트에서도 생성자 주입 패턴 사용 가능
     */
    private final ArticleRepository articleRepository;
    private final ArticleCommentRepository articleCommentRepository;

    @Autowired
    public JpaRepositoryTest(
            @Autowired ArticleRepository articleRepository,
            @Autowired ArticleCommentRepository articleCommentRepository
    ) {
        this.articleRepository = articleRepository;
        this.articleCommentRepository = articleCommentRepository;
    }

    @DisplayName("select 테스트")
    @Test
    void givenTestData_whenSelecting_thenWorksFine() {
        // Given

        // When
        List<Article> articles = articleRepository.findAll();

        // Then
        assertThat(articles)
                .isNotNull()
                .hasSize(123);
    }

    @DisplayName("insert 테스트")
    @Test
    void givenTestData_WhenInsert_thenWorkFine() {
        // given
        long previousCount = articleRepository.count();
        Article article = Article.of("new article", "new content", "#spring");

        // when
        Article savedArticle = articleRepository.save(article);

        // then
        assertThat(articleRepository.count()).isEqualTo(previousCount + 1);

     }

     @DisplayName("update 테스트")
     @Test
     void givenTestData_WhenUpdating_thenWorkFine() {
         // given
         Article article = articleRepository.findById(1L).orElseThrow();
         String updatedHashtag = "#springboot";
         article.setHashtag(updatedHashtag);

         // when
         // flush 를 해주면 로그로 업데이트 쿼리는 나가지만 실제 반영되지는 않음, 롤백됨
         Article savedArticle = articleRepository.saveAndFlush(article);

         // then
         // hashtag 필드가 업데이트 되었는가
         assertThat(savedArticle).hasFieldOrPropertyWithValue("hashtag", updatedHashtag);
      }

    @DisplayName("delete 테스트")
    @Test
    void givenTestData_WhenDeleting_thenWorkFine() {
        // given
        Article article = articleRepository.findById(1L).orElseThrow();
        long previousArticleCount = articleRepository.count();
        long previousArticleComment = articleCommentRepository.count();
        int deletedCommentsSize = article.getArticleComments().size();

        // when
        articleRepository.delete(article);

        // then
        assertThat(articleRepository.count()).isEqualTo(previousArticleCount - 1);
        assertThat(articleCommentRepository.count()).isEqualTo(previousArticleComment - deletedCommentsSize);
    }
}
