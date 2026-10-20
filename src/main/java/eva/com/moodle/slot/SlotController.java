package eva.com.moodle.slot;

import java.time.OffsetDateTime;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/slots")
@RequiredArgsConstructor
public class SlotController {
    private final SlotService slotService;

    @PostMapping("/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public SlotResponse create(@PathVariable Long userId, @Valid @RequestBody CreateSlotRequest request) {
        return slotService.create(userId, request);
    }

    @GetMapping("/{userId}")
    public Page<SlotResponse> list(@PathVariable Long userId,
                                   @RequestParam OffsetDateTime from,
                                   @RequestParam OffsetDateTime to,
                                   @RequestParam(required = false) SlotStatus status,
                                   @ParameterObject Pageable pageable) {
        if (from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wrong from-to values");
        }
        return slotService.find(userId, from, to, status, pageable);
    }
}
