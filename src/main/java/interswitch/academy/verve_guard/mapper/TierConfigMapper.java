package interswitch.academy.verve_guard.mapper;

import interswitch.academy.verve_guard.entities.TierConfig;
import interswitch.academy.verve_guard.models.response.TierConfigResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TierConfigMapper {
    TierConfigResponse map(TierConfig tierConfig);

    List<TierConfigResponse> map(List<TierConfig> tierConfigs);
}