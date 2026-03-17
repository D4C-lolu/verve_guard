package interswitch.academy.verve_guard.mapper;

import interswitch.academy.verve_guard.entities.User;
import interswitch.academy.verve_guard.models.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", source = "role.name")
    UserResponse map(User user);

    List<UserResponse> map(List<User> users);
}
