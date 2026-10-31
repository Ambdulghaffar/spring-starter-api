package com.ambdulghaffar.springstarterapi.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ambdulghaffar.springstarterapi.config.SecurityUtils;
import com.ambdulghaffar.springstarterapi.dto.MediaDto;
import com.ambdulghaffar.springstarterapi.entity.Media;
import com.ambdulghaffar.springstarterapi.entity.User;
import com.ambdulghaffar.springstarterapi.entity.enums.MediaContext;
import com.ambdulghaffar.springstarterapi.entity.enums.MediaType;
import com.ambdulghaffar.springstarterapi.properties.CloudinaryProperties;
import com.ambdulghaffar.springstarterapi.properties.MediaProperties;
import com.ambdulghaffar.springstarterapi.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private final MediaRepository mediaRepository;
    private final SecurityUtils securityUtils;
    private final Cloudinary cloudinary;
    private final CloudinaryProperties cloudinaryProperties;
    private final MediaProperties mediaProperties;

    @Override
    @Transactional
    public MediaDto.Response register(MediaDto.RegisterRequest request) {
        User user = securityUtils.getCurrentUser();
        String publicId = request.publicId();
        log.debug("Registering media {} for user id: {}", publicId, user.getId());

        String expectedPrefix = "https://res.cloudinary.com/" + cloudinaryProperties.cloudName()
                + "/" + resourceType(request.type()) + "/upload/";
        if (!request.url().startsWith(expectedPrefix) || !request.url().contains(publicId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL Cloudinary invalide");
        }

        boolean inUserFolder = publicId.startsWith(personalFolder(user.getId()) + "/");
        boolean inAdminContentFolder = isAdminContentFolder(publicId);

        if (inAdminContentFolder) {
            if (!securityUtils.hasRole("ADMIN")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Seuls les administrateurs peuvent enregistrer un média produit");
            }
        } else if (!inUserFolder) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Le média doit être uploadé dans le dossier de l'utilisateur");
        }

        if (mediaRepository.existsByPublicId(publicId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce média est déjà enregistré");
        }

        Media saved = mediaRepository.save(Media.builder()
                .publicId(publicId)
                .url(request.url())
                .type(request.type())
                .owner(user)
                .build());

        log.info("Media registered with id: {} for user id: {}", saved.getId(), user.getId());
        return MediaDto.Response.from(saved, false);
    }

    @Override
    public MediaDto.UploadSignature signUpload(MediaContext context) {
        Long userId = securityUtils.getCurrentUserId();

        String folder;
        if (context == MediaContext.PRIVATE) {
            folder = personalFolder(userId);
        } else {
            if (!securityUtils.hasRole("ADMIN")) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Seuls les administrateurs peuvent uploader du contenu global"
                );
            }

            folder = contentFolder();
        }

        long timestamp = System.currentTimeMillis() / 1000;

        Map<String, Object> params = new HashMap<>();
        params.put("folder", folder);
        params.put("timestamp", timestamp);

        String signature = cloudinary.apiSignRequest(params, cloudinaryProperties.apiSecret());
        log.info("Media upload signature generated for user id: {} (context: {})", userId, context);
        return new MediaDto.UploadSignature(signature, timestamp,
                cloudinaryProperties.apiKey(), cloudinaryProperties.cloudName(), folder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaDto.Response> getMyMedia() {
        Long userId = securityUtils.getCurrentUserId();

        Set<Long> unusedIds = mediaRepository.findUnusedByOwnerId(userId).stream()
                .map(Media::getId)
                .collect(Collectors.toSet());

        return mediaRepository.findByOwnerIdOrderByCreatedAtDesc(userId).stream()
                .map(media -> MediaDto.Response.from(media, !unusedIds.contains(media.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Media requiredOwnerOrAdmin(Long mediaId, Long ownerId, MediaType expectedType) {
        Optional<Media> found;
        if (securityUtils.hasRole("ADMIN")) {
            found = mediaRepository.findById(mediaId);
        } else {
            found = mediaRepository.findByIdAndOwnerId(mediaId, ownerId);
            if (found.isEmpty()) {
                found = mediaRepository.findById(mediaId)
                        .filter(media -> isAdminContentFolder(media.getPublicId()));
            }
        }

        Media media = found.orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aucun média trouvé pour l'utilisateur: " + ownerId + " avec l'ID: " + mediaId));

        if (media.getType() != expectedType) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le média " + mediaId + " n'est pas de type " + expectedType);
        }
        return media;
    }

    @Override
    public void deleteMyMedia(Long id, boolean force) {
        Long userId = securityUtils.getCurrentUserId();

        Optional<Media> found = securityUtils.hasRole("ADMIN")
                ? mediaRepository.findById(id)
                : mediaRepository.findByIdAndOwnerId(id, userId);

        Media media = found.orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun média trouvé avec l'ID: " + id));

        if (!force && isUsed(media)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce média est utilisé (post ou profil). Utilisez force=true pour le supprimer quand même.");
        }

        deleteMedia(media);
        log.info("Media {} deleted by user id: {}", id, userId);
    }

    @Override
    public MediaDto.DeleteResult deleteAllMyMedia(boolean unusedOnly, boolean force) {
        Long userId = securityUtils.getCurrentUserId();

        List<Media> targets = unusedOnly
                ? mediaRepository.findUnusedByOwnerId(userId)
                : mediaRepository.findByOwnerIdOrderByCreatedAtDesc(userId);

        int deleted = 0;
        int skipped = 0;
        int failed = 0;

        for (Media media : targets) {
            if (!force && isUsed(media)) {
                skipped++;
                continue;
            }
            try {
                deleteMedia(media);
                deleted++;
            } catch (Exception e) {
                log.warn("Failed to delete media {} : {}", media.getPublicId(), e.getMessage());
                failed++;
            }
        }

        log.info("Bulk media deletion for user id: {} - deleted: {}, skipped: {}, failed: {}",
                userId, deleted, skipped, failed);
        return new MediaDto.DeleteResult(deleted, skipped, failed);
    }

    @Override
    public void deleteAllOfUser(Long userId) {
        for (Media media : mediaRepository.findByOwnerIdOrderByCreatedAtDesc(userId)) {
            deleteMedia(media);
        }
        log.info("All media deleted for user id: {}", userId);
    }

    @Override
    @Scheduled(cron = "${media.orphan-purge-cron:0 0 3 * * *}")
    public void purgeOrphans() {
        LocalDateTime cutoff = LocalDateTime.now().minus(mediaProperties.orphanRetention());
        List<Media> orphans = mediaRepository.findUnusedCreatedBefore(cutoff);

        int deleted = 0;
        for (Media media : orphans) {
            try {
                deleteMedia(media);
                deleted++;
            } catch (Exception e) {
                log.warn("Orphan purge failed for media {} : {}", media.getPublicId(), e.getMessage());
            }
        }

        if (!orphans.isEmpty()) {
            log.info("Orphan media purge: {}/{} deleted", deleted, orphans.size());
        }
    }

    // Cloudinary d'abord, puis la base
    private void deleteMedia(Media media) {
        destroyOnCloudinary(media);
        mediaRepository.detachFromUserImages(media.getId());
        mediaRepository.delete(media);
    }

    private void destroyOnCloudinary(Media media) {
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(
                    media.getPublicId(),
                    ObjectUtils.asMap(
                            "resource_type", resourceType(media.getType()),
                            "invalidate", true
                    )
            );
            String status = String.valueOf(result.get("result"));

            if (!"ok".equals(status) && !"not found".equals(status)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Cloudinary : " + status);
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Cloudinary indisponible", e);
        } catch (RuntimeException e) {
            if (e instanceof ResponseStatusException) {
                throw e;
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Erreur Cloudinary : " + e.getMessage(), e);
        }
    }

    private boolean isUsed(Media media) {
        return mediaRepository.isUsedByUser(media.getId());
    }

    private boolean isAdminContentFolder(String publicId) {
        return publicId.startsWith(contentFolder() + "/");
    }

    private String resourceType(MediaType type) {
        return type == MediaType.VIDEO ? "video" : "image";
    }

    private String personalFolder(Long userId) {
        return cloudinaryProperties.rootFolder() + "/private/user_" + userId;
    }

    private String contentFolder() {
        return cloudinaryProperties.rootFolder() + "/content";
    }
}