package eva.com.moodle.meeting;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Embeddable
public record Participant(@NotBlank String name, @Email String email) {
}
