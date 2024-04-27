package com.example.projectboard.repository.querydsl;

import com.example.projectboard.domain.Article;
import com.example.projectboard.domain.QArticle;
import com.querydsl.jpa.JPQLQuery;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.List;

public class ArticleRepositoryCustomImpl extends QuerydslRepositorySupport implements ArticleRepositoryCustom {

    public ArticleRepositoryCustomImpl() {
        super(Article.class);
    }

    @Override
    public List<String> findAllDistinctHashtags() {
        QArticle article = QArticle.article;    // QArticle 에서 article 을 꺼내옴

        JPQLQuery<String> query = from(article) // from 으로 테이블을 잡아준다
                .distinct()
                .select(article.hashtag)        // 이 부분을 하기 위해 QueryDsl 사용, 특정 필드만 꺼내기
                .where(article.hashtag.isNotNull());

        return query.fetch();
    }

}
