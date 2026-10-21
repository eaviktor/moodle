package eva.com.moodle.slot;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/slots")
@RequiredArgsConstructor
public class SlotController {
    private final SlotService slotService;

    @PostMapping("/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public SlotResponse create(@PathVariable final Long userId, @Valid @RequestBody final CreateSlotRequest request) {
        return slotService.create(userId, request);
    }

    @GetMapping("/{userId}")
    public Page<SlotResponse> list(@PathVariable final Long userId,
                                   @RequestParam final OffsetDateTime from,
                                   @RequestParam final OffsetDateTime to,
                                   @RequestParam(required = false) final SlotStatus status,
                                   @ParameterObject final Pageable pageable) {
        if (from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wrong from-to values");
        }
        return slotService.find(userId, from, to, status, pageable);
    }

    @GetMapping("/{userId}/availableSlots")
    public UserSlotStatus availableSlots(@PathVariable final Long userId, @RequestParam final OffsetDateTime from, @RequestParam final OffsetDateTime to) {
        return slotService.availiableSlots(userId, from, to);
    }
}
