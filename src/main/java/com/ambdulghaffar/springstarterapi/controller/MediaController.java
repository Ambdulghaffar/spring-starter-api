package com.ambdulghaffar.springstarterapi.controller;

import com.ambdulghaffar.springstarterapi.dto.MediaDto;
import com.ambdulghaffar.springstarterapi.entity.enums.MediaContext;
import com.ambdulghaffar.springstarterapi.services.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Tag(name = "Media", description = "Médias personnels et contenus globaux administrés")
public class MediaController {

    private final MediaService mediaService;

    @GetMapping("/signature")
    @Operation(summary = "Paramètres signés pour envoyer un média personnel ou un contenu global")
    public ResponseEntity<MediaDto.UploadSignature> signature(
            @RequestParam(defaultValue = "PRIVATE") MediaContext context
    ) {
        return ResponseEntity.ok(mediaService.signUpload(context));
    }

    @GetMapping
    @Operation(summary = "List my media")
    public ResponseEntity<List<MediaDto.Response>> mine() {
        return ResponseEntity.ok(mediaService.getMyMedia());
    }

    @PostMapping
    @Operation(summary = "Register an uploaded media")
    public ResponseEntity<MediaDto.Response> register(@Valid @RequestBody MediaDto.RegisterRequest request) {
        MediaDto.Response response = mediaService.register(request);
        log.info("POST /api/v1/media - Media registered with id: {}", response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete one of my media (409 if in use, unless force=true)")
    public ResponseEntity<Void> delete(
            @PathVariable @Positive Long id,
            @RequestParam(defaultValue = "false") boolean force
    ) {
        mediaService.deleteMyMedia(id, force);
        log.info("DELETE /api/v1/media/{} - Media deleted", id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Delete several of my media (unused=true: only unused; force=true: also used ones)")
    public ResponseEntity<MediaDto.DeleteResult> deleteMine(
            @RequestParam(defaultValue = "false") boolean unused,
            @RequestParam(defaultValue = "false") boolean force
    ) {
        return ResponseEntity.ok(mediaService.deleteAllMyMedia(unused, force));
    }
}