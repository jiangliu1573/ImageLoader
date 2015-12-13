package com.cosmos.mark.imageloader.config;

import com.cosmos.mark.imageloader.R;

public class DisplayConfig {
    public int loadingResId = R.drawable.image_default;
    public int failedResId = R.drawable.image_default;

    public DisplayConfig() {

    }

    public DisplayConfig(int loadingResId, int failedResId) {
        this.loadingResId = loadingResId;
        this.failedResId = failedResId;
    }
}
