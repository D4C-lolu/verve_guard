package interswitch.academy.verve_guard.controllers.v1;

import interswitch.academy.verve_guard.constants.Permissions;
import interswitch.academy.verve_guard.constants.Roles;
import interswitch.academy.verve_guard.models.enums.CardStatus;
import interswitch.academy.verve_guard.models.request.CreateCardRequest;
import interswitch.academy.verve_guard.models.request.CreateMyCardRequest;
import interswitch.academy.verve_guard.models.response.CardResponse;
import interswitch.academy.verve_guard.services.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasRole('"+ Roles.ADMIN +"') or hasRole('" + Roles.SUPER_ADMIN + "')")
    public CardResponse createCard(@RequestBody @Valid CreateCardRequest request) {
        return cardService.createCard(request);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("me")
    @PreAuthorize("hasRole('"+ Roles.MERCHANT +"')")
    public CardResponse createCardForSelf(@RequestBody @Valid CreateMyCardRequest request) {
        return cardService.createCardForSelf(request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("{cardId}/block")
    @PreAuthorize("hasRole('"+ Roles.ADMIN +"') or hasRole('" + Roles.SUPER_ADMIN + "')")
    public void blockCard(@PathVariable String cardId) {
        cardService.blockCard(cardId);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("{cardId}/block/me")
    @PreAuthorize("hasRole('"+ Roles.MERCHANT +"')")
    public void blockCardForSelf(@PathVariable String cardId) {
        cardService.blockCardForSelf(cardId);
    }

    @GetMapping("{cardId}")
    @PreAuthorize("hasAuthority('" + Permissions.CARD_READ + "')")
    public CardResponse getCardById(@PathVariable String cardId) {
        return cardService.getCardById(cardId);
    }

    @GetMapping("account/{accountId}")
    @PreAuthorize("hasAuthority('" + Permissions.CARD_READ + "')")
    public Page<CardResponse> getCardsByAccount(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "created_at") String sortField,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection
    ) {
        return cardService.getCardsByAccount(accountId, page, size, sortField, sortDirection);
    }

    @PatchMapping("{cardId}/status")
    @PreAuthorize("hasAuthority('" + Permissions.CARD_UPDATE + "')")
    public CardResponse updateCardStatus(
            @PathVariable String cardId,
            @RequestParam CardStatus status
    ) {
        return cardService.updateCardStatus(cardId, status);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("{cardId}")
    @PreAuthorize("hasAuthority('" + Permissions.CARD_DELETE + "')")
    public void deleteCard(@PathVariable String cardId) {
        cardService.deleteCard(cardId);
    }
}