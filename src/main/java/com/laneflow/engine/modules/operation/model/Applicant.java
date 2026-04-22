package com.laneflow.engine.modules.operation.model;

import com.laneflow.engine.modules.operation.model.enums.ApplicantType;
import com.laneflow.engine.modules.operation.model.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "applicants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Applicant {

    @Id
    private String id;

    private ApplicantType type;
    private DocumentType documentType;

    @Indexed(unique = true)
    private String documentNumber;

    private String firstName;
    private String lastName;
    private String businessName;
    private String legalRepresentative;
    private String email;
    private String phone;
    private String address;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}
