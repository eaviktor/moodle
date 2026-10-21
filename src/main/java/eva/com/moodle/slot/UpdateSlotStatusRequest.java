package eva.com.moodle.slot;

import jakarta.validation.constraints.NotNull;

public record UpdateSlotStatusRequest(@NotNull SlotStatus status) {
}
