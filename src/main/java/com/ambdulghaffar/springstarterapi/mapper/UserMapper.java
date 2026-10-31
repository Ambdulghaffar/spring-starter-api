package com.ambdulghaffar.springstarterapi.mapper;

import com.ambdulghaffar.springstarterapi.dto.UserDto;
import com.ambdulghaffar.springstarterapi.entity.Media;
import com.ambdulghaffar.springstarterapi.entity.User;
import org.mapstruct.*;

import java.util.ArrayList;
import java.util.List;

// ℹ️ MapStruct automatically maps properties when source and target property names match,
// including nested properties. ⛔ Non-matching properties are ignored.

@Mapper(componentModel = "spring")
public interface UserMapper {

    // source (User) = the object MapStruct reads data from
    // target (UserDto.Response) = the object MapStruct writes data to
    //
    // ⛔ Use @Mapping(target = "targetProperty", source = "sourceProperty")
    // when source and target property names differ.
    // Example: @Mapping(target = "author", source = "user")
    @Mapping(target = "avatarUrl", source = "images", qualifiedByName = "avatarUrl")
    @Mapping(target = "images", source = "images", qualifiedByName = "imageDtos")
    UserDto.Response toDto(User user);

    @Mapping(target = "avatarUrl", source = "images", qualifiedByName = "avatarUrl")
    @Mapping(target = "images", source = "images", qualifiedByName = "imageDtos")
    UserDto.Summary toSummary(User user);

    // source (UserDto.Request) = the object MapStruct reads data from
    // target (User) = the object MapStruct writes data to
    //
    // "ignore = true" means that this target property must not be mapped.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "images", ignore = true)
    User toEntity(UserDto.Request request);

    // source (UserDto.Request) = the object MapStruct reads data from
    // target (User) = the existing object MapStruct writes data to
    //
    // Null values are ignored, so existing target values are kept
    // when the corresponding source property is null.
    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
    )
    @Mapping(target = "images", ignore = true)
    void updateEntity(
            UserDto.Request request, // The parameter without @MappingTarget is the source.
            @MappingTarget User user // @MappingTarget identifies the target to be modified.
    );

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "images", ignore = true)
    void updateEntity(UserDto.UpdateProfile request, @MappingTarget User user);

    @Named("avatarUrl")
    default String avatarUrl(List<Media> images) {
        return images == null || images.isEmpty() ? null : images.get(0).getUrl();
    }

    @Named("imageDtos")
    default List<UserDto.ImageDto> imageDtos(List<Media> images) {
        if (images == null) {
            return List.of();
        }
        List<UserDto.ImageDto> result = new ArrayList<>(images.size());
        for (int position = 0; position < images.size(); position++) {
            Media image = images.get(position);
            result.add(new UserDto.ImageDto(image.getId(), image.getUrl(), position));
        }
        return result;
    }
}
