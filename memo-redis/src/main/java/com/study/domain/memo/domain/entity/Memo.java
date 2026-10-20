package com.study.domain.memo.domain.entity;

import com.study.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 메모 관련 entity
 *
 * @author : choiseongho
 * @fileName : Memo.java
 * @since : 2025-09-17
 */
@Entity
@Table(name = "MEMO")
@Builder
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class Memo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    @Comment("메모 ID (Auto Increment)")
    private Long id;

    @Column(name = "TITLE", nullable = false)
    @Comment("메모 제목")
    private String title;

    @Column(name = "CONTENT", nullable = false)
    @Comment("메모 본문")
    private String content;

    @Column(name = "FAVORITE")
    @Comment("메모 즐겨찾기 여부")
    @Builder.Default
    private Boolean favorite = false;

    @Column(name = "VIEW_COUNT")
    @Comment("메모 조회수")
    @Builder.Default
    private Long viewCount = 0L;

    public Memo(String title, String content, boolean favorite, Long viewCount) {
        this.title = title;
        this.content = content;
        this.favorite = favorite;
        this.viewCount = viewCount;
    }


    @Override
    @PrePersist
    public void prePersist(){
        // 부모 클래스의 prePersist 메소드 호출 (createdAt, updatedAt 설정)
        super.prePersist();

        // favorite과 viewCount 초기화
        favorite = false;
        viewCount = 0L;
    }

    public boolean toggleFavorite() {
        this.favorite = !this.favorite;
        return this.favorite;
    }
}
