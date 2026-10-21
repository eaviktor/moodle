package eva.com.moodle.meeting;

import java.time.OffsetDateTime;
import java.util.List;

public record MeetingResponse(Long id, Long slotId, OffsetDateTime startTime, OffsetDateTime endTime, String title,
                              String description, List<Participant> participants) {
}
