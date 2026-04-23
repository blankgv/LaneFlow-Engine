package com.laneflow.engine.core.storage;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.stereotype.Component;

@Component
public class GcsClientProvider {

    private final Storage storage = StorageOptions.getDefaultInstance().getService();

    public Storage storage() {
        return storage;
    }
}
