package eva.com.moodle.slot;

import java.time.OffsetDateTime;

public record SlotResponse(Long id, OffsetDateTime startTime, OffsetDateTime endTime, SlotStatus status) {
}

