# MapStruct — Source & Target

> ℹ️ **Source** = the object MapStruct reads data from.
> **Target** = the object MapStruct writes data to.
>
> MapStruct automatically maps properties when source and target property names match, including nested properties.
> If the names differ, specify the mapping explicitly with `@Mapping(source, target)`.
> Non-matching properties are ignored.

# Nested Objects

> ℹ️ When both source and target contain corresponding nested properties, MapStruct tries to map the nested objects automatically.
>
> MapStruct can generate the nested mapping even when the nested object types are different, as long as their properties can be matched.
>
> If properties inside the nested objects have different names, the corresponding mappings must be specified with `@Mapping`.

**Example:**

```text
Source                          Target

Comment                         CommentDto.Response
└── author : User       →       └── author : UserDto.Summary
```

If the properties of `User` and `UserDto.Summary` have matching names, MapStruct can generate the nested mapping automatically.

If some properties have different names, those mappings must be explicitly defined.

# `uses`

> ℹ️ `uses` allows MapStruct to delegate a nested object mapping to another mapper.
>
> When `uses` is used, MapStruct delegates the conversion of the nested object to the specified mapper.

**Example:**

```java
@Mapper(uses = UserMapper.class)
public interface CommentMapper {

    CommentDto.Response toDto(Comment comment);
}
```

The nested mapping is delegated:

```text
CommentMapper
     │
     └── author : User
             ↓
        UserMapper
             ↓
     author : UserDto.Summary
```

The mapping rules for `User → UserDto.Summary` are then defined in `UserMapper`:

```java
@Mapper
public interface UserMapper {

    @Mapping(target = "profileImage", source = "avatarUrl")
    UserDto.Summary toSummary(User user);
}
```

Without `uses`, the nested mapping can instead be handled directly in the main mapper:

```java
@Mapper
public interface CommentMapper {

    @Mapping(target = "author.profileImage", source = "author.avatarUrl")
    CommentDto.Response toDto(Comment comment);
}
```

> ⚠️ `uses` is **not required** simply because a nested object exists.
> If MapStruct can generate the nested mapping automatically, `uses` is optional.
>
> If a nested property exists only in the source or only in the target, there is no corresponding property to map, so it is ignored.
>
> **In short:** `uses` tells MapStruct **which mapper should handle a nested object**, while `@Mapping` tells MapStruct **how properties with different names should be mapped**.
