package eva.com.moodle.slot;

import eva.com.moodle.common.BusinessException;
import eva.com.moodle.meeting.MeetingRepository;
import eva.com.moodle.slot.UserSlotStatus.TimeSlotPeriod;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SlotService {

    private final SlotRepository slotRepository;
    private final CalendarRepository calendarRepository;
    private final MeetingRepository meetingRepository;

    private static SlotResponse mapResponse(final Slot slot) {
        return new SlotResponse(slot.getId(), slot.getStartTime(), slot.getEndTime(), slot.getStatus());
    }

    @Transactional
    public SlotResponse create(final Long userId, final CreateSlotRequest request) {
        final Calendar calendar = calendarRepository.lockByUserId(userId);
        final Slot slotEntity = new Slot();
        slotEntity.setCalendar(calendar);
        slotEntity.setStartTime(request.startTime());
        slotEntity.setEndTime(request.startTime()
                .plus(Duration.ofMinutes(request.durationMinutes())));
        slotEntity.setStatus(request.status() != null ? request.status() : SlotStatus.FREE);

        requireNoOverlap(calendar, slotEntity.getStartTime(), slotEntity.getEndTime(), null);
        return mapResponse(slotRepository.saveAndFlush(slotEntity));
    }

    @Transactional
    public SlotResponse update(final Long userId, final Long slotId, final UpdateSlotRequest request) {
        final Calendar calendar = calendarRepository.lockByUserId(userId);
        final Slot slot = getSlotWithoutMeeting(calendar, slotId);
        final OffsetDateTime endTime = request.startTime()
                .plus(Duration.ofMinutes(request.durationMinutes()));

        requireNoOverlap(calendar, request.startTime(), endTime, slot.getId());
        slot.setStartTime(request.startTime());
        slot.setEndTime(endTime);
        return mapResponse(slot);
    }

    @Transactional
    public SlotResponse updateStatus(final Long userId, final Long slotId, final SlotStatus status) {
        final Calendar calendar = calendarRepository.lockByUserId(userId);
        final Slot slot = getSlotWithoutMeeting(calendar, slotId);
        slot.setStatus(status);
        return mapResponse(slot);
    }

    @Transactional
    public void delete(final Long userId, final Long slotId) {
        final Calendar calendar = calendarRepository.lockByUserId(userId);
        slotRepository.delete(getSlotWithoutMeeting(calendar, slotId));
    }

    @Transactional
    public Page<SlotResponse> find(final Long userId, final OffsetDateTime from, final OffsetDateTime to, final SlotStatus status, final Pageable pageable) {
        final EnumSet<SlotStatus> statuses = status == null ? EnumSet.allOf(SlotStatus.class) : EnumSet.of(status);

        return slotRepository.findInRange(calendarRepository.getByUserId(userId), statuses, from, to, pageable)
                .map(SlotService::mapResponse);
    }

    @Transactional
    public UserSlotStatus availiableSlots(final Long userId, final OffsetDateTime from, final OffsetDateTime to) {
        final List<Slot> searchableSlots = slotRepository.findInRange(calendarRepository.getByUserId(userId),
                        EnumSet.allOf(SlotStatus.class),
                        from,
                        to,
                        Pageable.unpaged())
                .getContent();
        final Map<SlotStatus, List<TimeSlotPeriod>> periods = new EnumMap<>(SlotStatus.class);
        for (final SlotStatus status : SlotStatus.values()) {
            periods.put(status, new ArrayList<>());
        }

        searchableSlots.forEach(slot -> {
            final OffsetDateTime start = slot.getStartTime()
                    .isBefore(from) ? from : slot.getStartTime();
            final OffsetDateTime end = slot.getEndTime()
                    .isAfter(to) ? to : slot.getEndTime();
            final List<TimeSlotPeriod> timeSlotPeriods = periods.get(slot.getStatus());

            if (!timeSlotPeriods.isEmpty() && timeSlotPeriods.getLast()
                    .endTime()
                    .isEqual(start)) {
                timeSlotPeriods.add(new TimeSlotPeriod(timeSlotPeriods.removeLast()
                        .startTime(), end));
            } else {
                timeSlotPeriods.add(new TimeSlotPeriod(start, end));
            }
        });

        return new UserSlotStatus(from, to, periods);
    }

    // Booked slots are not 'modifiable'
    private Slot getSlotWithoutMeeting(final Calendar calendar, final Long slotId) {
        final Slot slot = slotRepository.findByIdAndCalendar(slotId, calendar)
                .orElseThrow(() -> BusinessException.notFound("Slot", slotId));
        if (meetingRepository.existsBySlotId(slotId)) {
            throw BusinessException.conflict("Slot %d has a meeting".formatted(slotId));
        }
        return slot;
    }

    private void requireNoOverlap(final Calendar calendar, final OffsetDateTime start, final OffsetDateTime end,
                                  final Long ignoredSlotId) {
        final boolean overlaps = slotRepository.findInRange(calendar,
                        EnumSet.allOf(SlotStatus.class),
                        start,
                        end,
                        Pageable.unpaged())
                .stream()
                .anyMatch(other -> !other.getId()
                        .equals(ignoredSlotId));
        if (overlaps) {
            throw BusinessException.conflict("Slot overlaps another slot in the calendar");
        }
    }
}
