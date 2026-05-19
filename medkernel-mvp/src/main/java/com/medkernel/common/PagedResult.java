package com.medkernel.common;

import java.util.List;

/**
 * 统一分页响应。
 *
 * <p>字段契约（与前端 api/types.ts PagedResult 对齐）：
 * <ul>
 *   <li>items — 当前页数据</li>
 *   <li>total — 总记录数</li>
 *   <li>page — 当前页码（从 1 开始）</li>
 *   <li>page_size — 每页大小</li>
 *   <li>total_pages — 总页数</li>
 * </ul>
 */
public class PagedResult<T> {
    private List<T> items;
    private long total;
    private int page;
    private int pageSize;
    private int totalPages;

    public PagedResult() {}

    public PagedResult(List<T> items, long total, int page, int pageSize) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0;
    }

    /**
     * 从完整列表创建分页结果。
     */
    public static <T> PagedResult<T> of(List<T> allItems, int page, int pageSize) {
        long total = allItems.size();
        int totalPages = pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 1;
        int safePage = Math.max(1, Math.min(page, totalPages));
        int fromIndex = (safePage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, allItems.size());
        List<T> pageItems = fromIndex < allItems.size()
                ? allItems.subList(fromIndex, toIndex)
                : List.of();
        return new PagedResult<>(pageItems, total, safePage, pageSize);
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
