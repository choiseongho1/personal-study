package com.study.domain.memoTag.domain.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor @NoArgsConstructor
@EqualsAndHashCode
public class MemoTagId implements Serializable {
    private Long memoId;
    private Long tagId;
}
