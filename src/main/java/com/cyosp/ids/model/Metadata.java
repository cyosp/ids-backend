package com.cyosp.ids.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static lombok.AccessLevel.NONE;
import static lombok.AccessLevel.PRIVATE;

@Data
@Setter(value = NONE)
@NoArgsConstructor(access = PRIVATE)
@AllArgsConstructor
public class Metadata {
    private String takenAt;
}
