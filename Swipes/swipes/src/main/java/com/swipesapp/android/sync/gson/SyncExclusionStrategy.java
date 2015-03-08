package com.swipesapp.android.sync.gson;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

/**
 * Exclusion strategy to remove classes or fields annotated as @LocalOnly.
 */
public class SyncExclusionStrategy implements ExclusionStrategy {

    public boolean shouldSkipClass(Class<?> clazz) {
        return false;
    }

    public boolean shouldSkipField(FieldAttributes f) {
        return f.getAnnotation(LocalOnly.class) != null;
    }

}
