package com.ambdulghaffar.springstarterapi.services.ai;

import com.ambdulghaffar.springstarterapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DbTools {

    // Adapte les noms à tes repositories
    private final UserRepository users;

    @Tool(description = "Nombre de lignes d'une table : users, posts, comments ou addresses")
    public long countRows(@ToolParam(description = "users, posts, comments ou addresses") String table) {
        return switch (table) {
            case "users" -> users.count();
            default -> throw new IllegalArgumentException("Table inconnue : " + table);
        };
    }

    @Tool(description = "Les derniers utilisateurs inscrits (id et nom seulement)")
    public List<String> latestUsers(@ToolParam(description = "Nombre d'utilisateurs, 20 maximum") int limit) {
        var page = PageRequest.of(0, Math.min(limit, 20), Sort.by(Sort.Direction.DESC, "id"));
        return users.findAll(page).stream()
                .map(u -> u.getId() + " - " + u.getEmail())   // adapte getUsername()
                .toList();
    }
}