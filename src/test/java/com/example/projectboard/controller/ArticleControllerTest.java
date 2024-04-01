package com.example.projectboard.controller;

import com.example.projectboard.config.SecurityConfig;
import com.example.projectboard.domain.type.SearchType;
import com.example.projectboard.dto.ArticleWithCommentsDto;
import com.example.projectboard.dto.UserAccountDto;
import com.example.projectboard.service.ArticleService;
import com.example.projectboard.service.PaginationService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;

@DisplayName("View 컨트롤러 - 게시글")
@Import(SecurityConfig.class)
@WebMvcTest(ArticleController.class)
class ArticleControllerTest {

    private final MockMvc mvc;

    @MockBean private ArticleService articleService;    // MockBean 은 @Autowired 로 생성자 주입 안됨
    @MockBean private PaginationService paginationService;

    public ArticleControllerTest(@Autowired MockMvc mvc) {
        this.mvc = mvc;
    }

    @DisplayName("[view][GET] 게시글 리스트 (게시판) 페이지 - 정상 호출")
    @Test
    void given_whenRequestingArticlesView_thenReturnsArticleView() throws Exception {
        // given                                                // 필드 중 일부만 ArgumentMatcher 를 쓸 수 없다.
        BDDMockito.given(articleService.searchArticles(ArgumentMatchers.eq(null), ArgumentMatchers.eq(null), ArgumentMatchers.any(Pageable.class)))
                        .willReturn(Page.empty());
        BDDMockito.given(paginationService.getPaginationBarNumbers(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())).willReturn(List.of(0,1,2,3,4));

        // when & then
        mvc.perform(MockMvcRequestBuilders.get("/articles"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(MockMvcResultMatchers.view().name("articles/index"))
                .andExpect(MockMvcResultMatchers.model().attributeExists("articles"))
                .andExpect(MockMvcResultMatchers.model().attributeExists("paginationBarNumbers"));
        // should 는 1번 호출한다는 의미가 있음
        BDDMockito.then(articleService).should().searchArticles(ArgumentMatchers.eq(null), ArgumentMatchers.eq(null), ArgumentMatchers.any(Pageable.class));
        BDDMockito.then(paginationService).should().getPaginationBarNumbers(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt());
     }

    @DisplayName("[view][GET] 게시글 리스트 (게시판) 페이지 - 검색어와 함께 호출")
    @Test
    void givenSearchKeyword_whenSearchingArticlesView_thenReturnsArticlesView() throws Exception {
        // given
        SearchType searchType = SearchType.TITLE;
        String searchValue = "title";
        BDDMockito.given(articleService.searchArticles(ArgumentMatchers.eq(searchType), ArgumentMatchers.eq(searchValue), ArgumentMatchers.any(Pageable.class)))
                .willReturn(Page.empty());
        BDDMockito.given(paginationService.getPaginationBarNumbers(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())).willReturn(List.of(0,1,2,3,4));

        // when & then
        mvc.perform(
                MockMvcRequestBuilders.get("/articles")
                        .queryParam("searchType", searchType.name())
                        .queryParam("searchValue", searchValue)
                )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(MockMvcResultMatchers.view().name("articles/index"))
                .andExpect(MockMvcResultMatchers.model().attributeExists("articles"))
                .andExpect(MockMvcResultMatchers.model().attributeExists("searchTypes"));

        BDDMockito.then(articleService).should().searchArticles(ArgumentMatchers.eq(searchType), ArgumentMatchers.eq(searchValue), ArgumentMatchers.any(Pageable.class));
        BDDMockito.then(paginationService).should().getPaginationBarNumbers(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt());
    }

    @DisplayName("[view][GET] 게시글 페이지 - 정상 호출")
    @Test
    void givenNothing_whenRequestingArticleView_thenReturnsArticleView() throws Exception {
        // given
        Long articleId = 1L;
        long totalCount = 1L;
        BDDMockito.given(articleService.getArticle(articleId)).willReturn(createArticleWithCommentsDto());
        BDDMockito.given(articleService.getArticleCount()).willReturn(totalCount);

        // when & then
        mvc.perform(MockMvcRequestBuilders.get("/articles/" + articleId))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(MockMvcResultMatchers.view().name("articles/detail"))
                .andExpect(MockMvcResultMatchers.model().attributeExists("article"))
                .andExpect(MockMvcResultMatchers.model().attributeExists("articleComments"))
                .andExpect(MockMvcResultMatchers.model().attributeExists("totalCount"));

        BDDMockito.then(articleService).should().getArticle(articleId);
        BDDMockito.then(articleService).should().getArticleCount();
    }

    @DisplayName("[view][GET] 게시글 리스트 (게시판) 페이지 - 페이징, 정렬 기능")
    @Test
    void givenPagingAndSortingParams_whenSearchingArticlesView_thenReturnsArticlesView() throws Exception {
        // Given
        String sortName = "title";
        String direction = "desc";
        int pageNumber = 0;
        int pageSize = 5;
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Order.desc(sortName)));
        List<Integer> barNumbers = List.of(1, 2, 3, 4, 5);
        given(articleService.searchArticles(null, null, pageable)).willReturn(Page.empty());
        given(paginationService.getPaginationBarNumbers(pageable.getPageNumber(), Page.empty().getTotalPages())).willReturn(barNumbers);

        // When & Then
        mvc.perform(
                        get("/articles")
                                .queryParam("page", String.valueOf(pageNumber))
                                .queryParam("size", String.valueOf(pageSize))
                                .queryParam("sort", sortName + "," + direction)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(view().name("articles/index"))
                .andExpect(model().attributeExists("articles"))
                .andExpect(model().attribute("paginationBarNumbers", barNumbers));
        then(articleService).should().searchArticles(null, null, pageable);
        then(paginationService).should().getPaginationBarNumbers(pageable.getPageNumber(), Page.empty().getTotalPages());
    }

    @Disabled("구현 중")
    @DisplayName("[view][GET] 게시글 검색 전용 페이지 - 정상 호출")
    @Test
    void given_whenRequestingArticleSearchView_thenReturnsArticleSearchView() throws Exception {
        // given

        // when & then
        mvc.perform(MockMvcRequestBuilders.get("/articles/search"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(MockMvcResultMatchers.view().name("articles/search"));
    }

    @DisplayName("[view][GET] 게시글 해시태그 검색 페이지 - 정상 호출")
    @Test
    void givenNothing_whenRequestingArticleSearchHashtagView_thenReturnsArticleSearchHashtagView() throws Exception {
        // given
        List<String> hashtags = List.of("#java", "#spring", "#boot");
        BDDMockito.given(articleService.searchArticlesViaHashtag(ArgumentMatchers.eq(null), ArgumentMatchers.any(Pageable.class)))
                .willReturn(Page.empty());
        BDDMockito.given(paginationService.getPaginationBarNumbers(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())).willReturn(List.of(1,2,3,4,5));
        BDDMockito.given(articleService.getHashtags()).willReturn(hashtags);


        // when & then
        mvc.perform(MockMvcRequestBuilders.get("/articles/search-hashtag"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(MockMvcResultMatchers.view().name("articles/search-hashtag"))
                .andExpect(MockMvcResultMatchers.model().attribute("articles", Page.empty()))
                .andExpect(MockMvcResultMatchers.model().attribute("hashtags", hashtags))
                .andExpect(MockMvcResultMatchers.model().attributeExists("paginationBarNumbers"))
                .andExpect(MockMvcResultMatchers.model().attribute("searchType", SearchType.HASHTAG));

        BDDMockito.then(articleService).should().searchArticlesViaHashtag(ArgumentMatchers.eq(null), ArgumentMatchers.any(Pageable.class));
        BDDMockito.then(articleService).should().getHashtags();
        BDDMockito.then(paginationService).should().getPaginationBarNumbers(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt());
    }

    @DisplayName("[view][GET] 게시글 해시태그 검색 페이지 - 정상 호출, 해시태그 입력")
    @Test
    void givenHashtag_whenRequestingArticleSearchHashtagView_thenReturnsArticleSearchHashtagView() throws Exception {
        // given
        String hashtag = "#java";
        List<String> hashtags = List.of("#java", "#spring", "#boot");
        BDDMockito.given(articleService.searchArticlesViaHashtag(ArgumentMatchers.eq(hashtag), ArgumentMatchers.any(Pageable.class)))
                .willReturn(Page.empty());
        BDDMockito.given(paginationService.getPaginationBarNumbers(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())).willReturn(List.of(1,2,3,4,5));
        BDDMockito.given(articleService.getHashtags()).willReturn(hashtags);

        // when & then
        mvc.perform(
                MockMvcRequestBuilders.get("/articles/search-hashtag")
                        .queryParam("searchValue", hashtag)
                )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(MockMvcResultMatchers.view().name("articles/search-hashtag"))
                .andExpect(MockMvcResultMatchers.model().attribute("articles", Page.empty()))
                .andExpect(MockMvcResultMatchers.model().attribute("hashtags", hashtags))
                .andExpect(MockMvcResultMatchers.model().attributeExists("paginationBarNumbers"))
                .andExpect(MockMvcResultMatchers.model().attribute("searchType", SearchType.HASHTAG));

        BDDMockito.then(articleService).should().searchArticlesViaHashtag(ArgumentMatchers.eq(hashtag), ArgumentMatchers.any(Pageable.class));
        BDDMockito.then(articleService).should().getHashtags();
        BDDMockito.then(paginationService).should().getPaginationBarNumbers(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt());
    }

    private ArticleWithCommentsDto createArticleWithCommentsDto() {
        return ArticleWithCommentsDto.of(
                1L,
                createUserAccountDto(),
                Set.of(),
                "title",
                "content",
                "#java",
                LocalDateTime.now(),
                "lbk",
                LocalDateTime.now(),
                "lbk"
        );
    }

    private UserAccountDto createUserAccountDto() {
        return UserAccountDto.of(
                "lbk",
                "pw",
                "lbk@gmail.com",
                "Forest",
                "memo",
                LocalDateTime.now(),
                "lbk",
                LocalDateTime.now(),
                "lbk"
        );
    }
}
