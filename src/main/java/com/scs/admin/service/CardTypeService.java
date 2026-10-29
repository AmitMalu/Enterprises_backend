package com.scs.admin.service;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface CardTypeService {

    ByteArrayResource generateSampleTemplate() throws IOException;

    String uploadCardTypes(MultipartFile file) throws IOException;

}
