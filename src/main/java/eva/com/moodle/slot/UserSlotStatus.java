package eva.com.moodle.slot;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record UserSlotStatus(OffsetDateTime from, OffsetDateTime to, Map<SlotStatus, List<TimeSlotPeriod>> periods) {

    public record TimeSlotPeriod(OffsetDateTime startTime, OffsetDateTime endTime) {
    }
}
