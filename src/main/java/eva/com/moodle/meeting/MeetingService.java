package eva.com.moodle.meeting;

import eva.com.moodle.common.BusinessException;
import eva.com.moodle.slot.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final SlotRepository slotRepository;
    private final CalendarRepository calendarRepository;

    private static MeetingResponse mapResponse(final Meeting meeting) {
        final Slot slot = meeting.getSlot();
        return new MeetingResponse(meeting.getId(), slot.getId(), slot.getStartTime(), slot.getEndTime(),
                meeting.getTitle(), meeting.getDescription(), List.copyOf(meeting.getParticipants()));
    }

    @Transactional
    public MeetingResponse schedule(final Long userId, final ScheduleMeetingRequest request) {
        // Lock before loading slot
        final Calendar calendar = calendarRepository.lockByUserId(userId);
        final Slot slot = slotRepository.findByIdAndCalendar(request.slotId(), calendar)
                .orElseThrow(() -> BusinessException.notFound("Slot", request.slotId()));
        if (slot.getStatus() != SlotStatus.FREE) {
            throw BusinessException.conflict("Slot %d is not free".formatted(slot.getId()));
        }
        slot.setStatus(SlotStatus.BOOKED);

        final Meeting meeting = new Meeting();
        meeting.setSlot(slot);
        meeting.setTitle(request.title());
        meeting.setDescription(request.description());
        meeting.setParticipants(emptyIfNull(request.participants()));
        return mapResponse(meetingRepository.saveAndFlush(meeting));
    }

    @Transactional(readOnly = true)
    public MeetingResponse get(final Long userId, final Long meetingId) {
        final Calendar calendar = calendarRepository.getByUserId(userId);
        return meetingRepository.findByIdAndSlotCalendar(meetingId, calendar)
                .map(MeetingService::mapResponse)
                .orElseThrow(() -> BusinessException.notFound("Meeting", meetingId));
    }
}
