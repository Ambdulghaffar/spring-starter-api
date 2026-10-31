package com.ambdulghaffar.springstarterapi.services;

import com.ambdulghaffar.springstarterapi.dto.MediaDto;
import com.ambdulghaffar.springstarterapi.entity.Media;
import com.ambdulghaffar.springstarterapi.entity.enums.MediaContext;
import com.ambdulghaffar.springstarterapi.entity.enums.MediaType;

import java.util.List;

public interface MediaService {
    MediaDto.Response register(MediaDto.RegisterRequest request);
    MediaDto.UploadSignature signUpload(MediaContext context);
    List<MediaDto.Response> getMyMedia();
    void deleteMyMedia(Long id, boolean force);
    MediaDto.DeleteResult deleteAllMyMedia(boolean unusedOnly, boolean force);
    Media requiredOwnerOrAdmin(Long mediaId, Long ownerId, MediaType expectedType);
    void deleteAllOfUser(Long userId);
    void purgeOrphans();
}