package com.scs.admin.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsId implements Serializable {

    private String mobileNumber;
    private String hierarchyId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserDetailsId that)) return false;
        return Objects.equals(mobileNumber, that.mobileNumber) &&
                Objects.equals(hierarchyId, that.hierarchyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mobileNumber, hierarchyId);
    }
}
