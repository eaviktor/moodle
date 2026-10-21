package eva.com.moodle.slot;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.OffsetDateTime;

public record UpdateSlotRequest(@NotNull OffsetDateTime startTime, @NotNull @Positive Integer durationMinutes) {
}
