package com.example.projectboard.dto.response;

import com.example.projectboard.dto.ArticleCommentDto;
import com.example.projectboard.dto.ArticleWithCommentsDto;
import com.example.projectboard.dto.HashtagDto;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public record ArticleWithCommentsResponse(
        Long id,
        String title,
        String content,
        Set<String> hashtags,
        LocalDateTime createdAt,
        String email,
        String nickname,
        String userId,
        Set<ArticleCommentResponse> articleCommentsResponse
) {
    public static ArticleWithCommentsResponse of(Long id, String title, String content, Set<String> hashtags, LocalDateTime createdAt, String email, String nickname, String userId, Set<ArticleCommentResponse> articleCommentResponses) {
        return new ArticleWithCommentsResponse(id, title, content, hashtags, createdAt, email, nickname, userId, articleCommentResponses);
    }

    public static ArticleWithCommentsResponse from(ArticleWithCommentsDto dto) {
        String nickname = dto.userAccountDto().nickname();
        if (nickname == null || nickname.isBlank()) {
            nickname = dto.userAccountDto().userId();
        }

        return new ArticleWithCommentsResponse(
                dto.id(),
                dto.title(),
                dto.content(),
                dto.hashtagDtos().stream()
                        .map(HashtagDto::hashtagName)
                        .collect(Collectors.toUnmodifiableSet())
                ,
                dto.createdAt(),
                dto.userAccountDto().email(),
                nickname,
                dto.userAccountDto().userId(),
                organizeChildComments(dto.articleCommentDtos())
        );
    }

    private static Set<ArticleCommentResponse> organizeChildComments(Set<ArticleCommentDto> dtos) {
        // Set 은 데이터에 접근할 수 있는 방법을 제공하지 않는다. 그래서 Map 을 이용
        Map<Long, ArticleCommentResponse> map = dtos.stream()
                .map(ArticleCommentResponse::from)
                .collect(Collectors.toMap(ArticleCommentResponse::id, Function.identity()));

        map.values().stream()
                .filter(ArticleCommentResponse::hasParentComment)   // 자식 댓글만 가져옴
                .forEach(comment -> {   // 순회
                    ArticleCommentResponse parentComment = map.get(comment.parentCommentId());  // 부모 댓글에 접근
                    parentComment.childComments().add(comment); // 부모 댓글의 childComments 필드에 자식 댓글 추가
                });


        return map.values().stream()
                .filter(comment -> !comment.hasParentComment()) // 부모 댓글만 가져옴,
                .collect(Collectors.toCollection(() ->    // 댓글 정렬과 대댓글 정렬이 따로 되어야 한다. 대댓글의 정렬은 ArticleCommentResponse 에서 이루어짐
                        new TreeSet<>(Comparator
                                .comparing(ArticleCommentResponse::createdAt)
                                .reversed() // createdAt 내림차순 정렬
                                .thenComparingLong(ArticleCommentResponse::id)  // 그다음 정렬 기준 id
                        )
                ));
    }

}
