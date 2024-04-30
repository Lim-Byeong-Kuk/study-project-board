package com.example.projectboard.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import javax.persistence.*;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@ToString(callSuper = true)
@Table(indexes = {
        @Index(columnList = "title"),
        @Index(columnList = "createdAt"),
        @Index(columnList = "createdBy")
})
@Entity
public class Article extends AuditingFields{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @JoinColumn(name = "userId")
    @ManyToOne(optional = false)
    private UserAccount userAccount;    // 유저 정보 (ID)

    @Setter
    @Column(nullable = false)
    private String title; // 제목

    @Setter
    @Column(nullable = false, length = 10000)
    private String content; // 본문


    @ToString.Exclude
    @JoinTable(             // 연관관계의 주인:hashtags 에서 주는 설정 값 설정하기
            name = "article_hashtag",   // 생성될 테이블 이름
            joinColumns = @JoinColumn(name = "articleId"),  // Article 쪽에서 조인 컬럼이름 지정
            inverseJoinColumns = @JoinColumn(name = "hashtagId")    // Hashtag 쪽에서 조인 컬럼이름 지정
    )
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}) // PERSIST 는 INSERT, MERGE 는 UPDATE 의미
    private Set<Hashtag> hashtags = new LinkedHashSet<>();


    @ToString.Exclude // 순환 참조 방지
    @OrderBy("createdAt DESC") // 정렬 기준 시간정렬 내림차순
    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL)
    private final Set<ArticleComment> articleComments = new LinkedHashSet<>();

    protected Article() {}

    private Article(UserAccount userAccount, String title, String content) {
        this.userAccount = userAccount;
        this.title = title;
        this.content = content;
    }

    // 팩토리 메서드
    public static Article of(UserAccount userAccount, String title, String content) {
        return new Article(userAccount, title, content);
    }

    public void addHashtag(Hashtag hashtag) {
        this.getHashtags().add(hashtag);
    }

    public void addHashtags(Collection<Hashtag> hashtags) {
        this.getHashtags().addAll(hashtags);
    }

    public void clearHashtags() {
        this.getHashtags().clear();
    }

    /**
     *  Entity 에서는 롬복이 아닌 독특한 방법으로
     *  equals ANd hashCode 를 만들어야한다.
     *  일단 id 만 비교하면 된다.
     *  id != null 의미
     *  영속화 되지 않은 엔티티는 동등석 비교에서 탈락한다.
     *  (그 내용이 동일하더라도)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Article that)) return false;
        return this.getId() != null && this.getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getId());
    }

}
