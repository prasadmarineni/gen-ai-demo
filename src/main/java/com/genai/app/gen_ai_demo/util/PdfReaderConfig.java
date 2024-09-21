package com.genai.app.gen_ai_demo.util;

public class PdfReaderConfig {
    private boolean enableImageExtraction;
    private boolean ignoreEmptyPages;
    private int maxRetries;

    // Getters and Setters
    public boolean isEnableImageExtraction() {
        return enableImageExtraction;
    }

    public void setEnableImageExtraction(boolean enableImageExtraction) {
        this.enableImageExtraction = enableImageExtraction;
    }

    public boolean isIgnoreEmptyPages() {
        return ignoreEmptyPages;
    }

    public void setIgnoreEmptyPages(boolean ignoreEmptyPages) {
        this.ignoreEmptyPages = ignoreEmptyPages;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
}

