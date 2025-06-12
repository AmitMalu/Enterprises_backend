package com.im.admin.entities;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Data
public class ImSubMasterId implements Serializable {

    private String imMasterId; // First part of the primary key
    private Integer imSubMasterId; // Second part of the primary key

}
