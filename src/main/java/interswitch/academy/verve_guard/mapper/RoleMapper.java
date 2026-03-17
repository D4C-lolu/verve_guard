package interswitch.academy.verve_guard.mapper;

import interswitch.academy.verve_guard.entities.Role;
import interswitch.academy.verve_guard.models.response.RoleResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    RoleResponse map(Role role);

    List<RoleResponse> map(List<Role> roles);
}

