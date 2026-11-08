package com.insipidknight.toarrive.user.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PageVO<T> {

    private List<T> list;

    private long total;

    private int page;

    private int size;
}
