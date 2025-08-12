package com.im.admin.service;

import java.util.List;

public interface UserMappingService {
    List<?> remapUsers( String fromId, String toId);
}
