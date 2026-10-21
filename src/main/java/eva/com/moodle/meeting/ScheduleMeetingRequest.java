package eva.com.moodle.meeting;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ScheduleMeetingRequest(@NotNull Long slotId, @NotBlank String title, String description,
                                     @NotEmpty List<@NotNull @Valid Participant> participants) {
}
