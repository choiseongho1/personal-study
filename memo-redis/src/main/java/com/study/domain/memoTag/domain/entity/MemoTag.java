package com.study.domain.memoTag.domain.entity;

import com.study.domain.memo.domain.entity.Memo;
import com.study.domain.tag.domain.entity.Tag;
import com.study.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Entity
@Table(name = "MEMO_TAG")
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@IdClass(MemoTagId.class)
public class MemoTag extends BaseEntity {
    @Id
    @Column(name = "MEMO_ID")
    @Comment("메모 ID")
    private Long memoId;

    @Id
    @Column(name = "TAG_ID")
    @Comment("태그 ID")
    private Long tagId;


    // MEMO 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMO_ID", insertable = false, updatable = false)
    private Memo memo;

    // TAG 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TAG_ID", insertable = false, updatable = false)
    private Tag tag;

}
