package com.scs.admin.util;

/**
 * @author SHUBHAM KHOPADE
 */

import java.time.LocalDateTime;

public interface UnderHierarchyProjection {

    String getHierarchyId();

    String getHierarchyType();

    String getParentHierarchyId();

    LocalDateTime getCreatedOn();

    Integer getDepth();
}
