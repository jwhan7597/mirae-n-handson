package com.example.item;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 문항 검색 API. 레거시 {@code search.php} 를 옮긴 것으로, 파라미터는 문자열 그대로 서비스에 넘긴다.
 */
@RestController
@RequestMapping("/api")
public class ItemSearchController {

    private final ItemSearchService itemSearchService;

    public ItemSearchController(ItemSearchService itemSearchService) {
        this.itemSearchService = itemSearchService;
    }

    /** {@code GET /api/items/search} — q, unit, level, tag, sort, dir, page. 잘못된 값도 400 으로 거부하지 않는다. */
    @GetMapping("/items/search")
    public ItemSearchResponse search(HttpServletRequest request) {
        return itemSearchService.search(ItemSearchRequest.fromQueryString(request.getQueryString()));
    }
}
