package ru.yandex.practicum.analyzer.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "conditions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Condition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type")
    private String type;

    @Column(name = "operation")
    private String operation;

    @Column(name = "value")
    private Integer value;
}