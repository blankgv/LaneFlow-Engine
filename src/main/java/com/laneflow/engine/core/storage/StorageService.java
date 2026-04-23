package com.laneflow.engine.core.storage;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final GcsClientProvider provider;

    @Value("${gcp.storage.bucket}")
    private String bucketName;

    public StoredObject upload(MultipartFile file, String objectName) {
        try {
            BlobId blobId = BlobId.of(bucketName, objectName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(resolveContentType(file))
                    .build();
            Blob blob = provider.storage().create(blobInfo, file.getBytes());
            return new StoredObject(
                    bucketName,
                    objectName,
                    blob.getContentType(),
                    blob.getSize(),
                    blob.getMediaLink()
            );
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el archivo a subir.", e);
        }
    }

    public void delete(String objectName) {
        Storage storage = provider.storage();
        storage.delete(BlobId.of(bucketName, objectName));
    }

    private String resolveContentType(MultipartFile file) {
        if (file.getContentType() == null || file.getContentType().isBlank()) {
            return "application/octet-stream";
        }
        return file.getContentType();
    }
}
