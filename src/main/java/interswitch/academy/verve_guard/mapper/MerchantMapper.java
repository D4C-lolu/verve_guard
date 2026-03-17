package interswitch.academy.verve_guard.mapper;

import interswitch.academy.verve_guard.models.projections.MerchantProjection;
import interswitch.academy.verve_guard.models.response.MerchantResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MerchantMapper {
    MerchantResponse map(MerchantProjection projection);

    List<MerchantResponse> map(List<MerchantProjection> projections);
}