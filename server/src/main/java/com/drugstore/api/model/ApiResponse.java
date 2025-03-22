package com.drugstore.api.model;

public class ApiResponse<T> {
    private T data;
    private int code;
    private String message;
    private long total;
    private int page;
    private int size;

    public ApiResponse(T data, int code, String message) {
        this.data = data;
        this.code = code;
        this.message = message;
    }

    // Getters and Setters
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
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
    public int getSize() {
        return size;
    }
    public void setSize(int size) {
        this.size = size;
    }
} 