package com.zanchi.zanchi_backend.domain.preference;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class MemberPreferenceId implements Serializable {
    private Long memberId;
    private Long preferenceTagId;
}
