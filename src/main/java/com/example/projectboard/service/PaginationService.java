package com.example.projectboard.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

@Service
public class PaginationService {

    private static final int BAR_LENGTH = 5;


    public List<Integer> getPaginationBarNumbers(int currentPageNumber, int totalPages) {
        // 초기 페이지네이션 바에서 startNumber 가 음수로 가지 않도록
        int startNumber = Math.max(currentPageNumber - (BAR_LENGTH / 2), 0);
        // 마지막 페이지네이션 바에서 endNumber 가 totalPages 를 초과하지 않도록
        int endNumber = Math.min(startNumber + BAR_LENGTH, totalPages);

        return IntStream.range(startNumber, endNumber).boxed().toList();
    }

    public int currentBarLength() {
        return BAR_LENGTH;
    }

}
