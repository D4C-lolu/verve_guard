package interswitch.academy.verve_guard.mapper;

import interswitch.academy.verve_guard.entities.Permission;
import interswitch.academy.verve_guard.models.response.PermissionResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

    PermissionResponse map(Permission permission);

    List<PermissionResponse> map(List<Permission> permissions);
}