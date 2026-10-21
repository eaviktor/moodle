package eva.com.moodle.slot;

import eva.com.moodle.common.BusinessException;
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

    @Transactional
    public SlotResponse create(final Long userId, final CreateSlotRequest request) {
        final Calendar calendar = calendarRepository.lockByUserId(userId);
        final Slot slotEntity = new Slot();
        slotEntity.setCalendar(calendar);
        slotEntity.setStartTime(request.startTime());
        slotEntity.setEndTime(request.startTime()
                .plus(Duration.ofMinutes(request.durationMinutes())));
        slotEntity.setStatus(request.status() != null ? request.status() : SlotStatus.FREE);

        requireNoOverlap(slotEntity);
        final Slot slot = slotRepository.saveAndFlush(slotEntity);
        return new SlotResponse(slot.getId(), slot.getStartTime(), slot.getEndTime(), slot.getStatus());
    }

    @Transactional
    public Page<SlotResponse> find(final Long userId, final OffsetDateTime from, final OffsetDateTime to, final SlotStatus status, final Pageable pageable) {
        final EnumSet<SlotStatus> statuses = status == null ? EnumSet.allOf(SlotStatus.class) : EnumSet.of(status);

        return slotRepository.findInRange(calendarRepository.getByUserId(userId), statuses, from, to, pageable)
                .map(slot -> new SlotResponse(slot.getId(), slot.getStartTime(), slot.getEndTime(), slot.getStatus()));
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

    private void requireNoOverlap(final Slot slot) {
        final boolean overlaps = slotRepository.findInRange(slot.getCalendar(),
                        EnumSet.allOf(SlotStatus.class),
                        slot.getStartTime(),
                        slot.getEndTime(),
                        Pageable.unpaged())
                .stream()
                .anyMatch(other -> !other.getId()
                        .equals(slot.getId()));
        if (overlaps) {
            throw BusinessException.conflict("Slot overlaps another slot in the calendar");
        }
    }
}
