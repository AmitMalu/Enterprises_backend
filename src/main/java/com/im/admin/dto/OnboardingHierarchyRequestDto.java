package com.im.admin.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OnboardingHierarchyRequestDto {

    private String hierarchyId;
    private String rejectionReason;
    private Boolean isApproved;

}
