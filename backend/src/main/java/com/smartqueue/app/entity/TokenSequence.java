package com.smartqueue.app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "token_sequences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenSequence {
    @Id
    @Column(name = "sector_type", nullable = false)
    private String sectorType; // HOSPITAL, BANK, COLLEGE

    @Column(name = "sequence_value", nullable = false)
    private Integer lastValue;
}
