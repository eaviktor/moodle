package eva.com.moodle.slot;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;

public record CreateSlotRequest(@NotNull OffsetDateTime startTime, @NotNull Integer durationMinutes, SlotStatus status) {
}