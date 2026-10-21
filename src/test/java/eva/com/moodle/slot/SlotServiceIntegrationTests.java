package eva.com.moodle.slot;

import eva.com.moodle.TestcontainersConfiguration;
import eva.com.moodle.common.BusinessException;
import eva.com.moodle.meeting.MeetingRepository;
import eva.com.moodle.slot.UserSlotStatus.TimeSlotPeriod;
import eva.com.moodle.user.User;
import eva.com.moodle.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SlotServiceIntegrationTests {

    private static final long UNKNOWN_USER_ID = Long.MAX_VALUE;

    @Autowired
    private SlotService slotService;
    @Autowired
    private SlotRepository slotRepository;
    @Autowired
    private CalendarRepository calendarRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MeetingRepository meetingRepository;

    private Calendar calendar;
    private Long userId;

    private static OffsetDateTime at(String time) {
        return OffsetDateTime.of(2026, 1, 5, 0, 0, 0, 0, ZoneOffset.UTC).with(LocalTime.parse(time));
    }

    private static TimeSlotPeriod period(String start, String end) {
        return new TimeSlotPeriod(at(start), at(end));
    }

    @BeforeEach
    void setUp() {
        meetingRepository.deleteAllInBatch();
        slotRepository.deleteAllInBatch();
        calendarRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        calendar = createCalendar("owner@example.com");
        userId = calendar.getUser().getId();
    }

    @Test
    void createPersistsFreeSlotEndingAfterDuration() {
        SlotResponse testResponse = slotService.create(userId, new CreateSlotRequest(at("09:00"), 45, null));

        assertThat(testResponse.id()).isNotNull();
        assertThat(testResponse.startTime()).isEqualTo(at("09:00"));
        assertThat(testResponse.endTime()).isEqualTo(at("09:45"));
        assertThat(testResponse.status()).isEqualTo(SlotStatus.FREE);
        Slot databaseResult = slotRepository.findByIdAndCalendar(testResponse.id(), calendar).get();

        assertThat(databaseResult.getStartTime()).isEqualTo(at("09:00"));
        assertThat(databaseResult.getEndTime()).isEqualTo(at("09:45"));
        assertThat(databaseResult.getStatus()).isEqualTo(SlotStatus.FREE);
    }

    @ParameterizedTest
    @CsvSource({
            "09:30, 60",
            "10:30, 60",
            "10:15, 30",
            "09:00, 180",
            "10:00, 60",
    })
    void rejectOverlappingIfExist(String start, int durationMinutes) {
        saveSlot(calendar, "10:00", "11:00", SlotStatus.BOOKED);

        assertThatThrownBy(() -> slotService.create(userId, new CreateSlotRequest(at(start), durationMinutes, null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

        assertThat(slotRepository.count()).isEqualTo(1);
    }

    @Test
    void allowsSlotsOnTheEdges() {
        saveSlot(calendar, "10:00", "11:00", SlotStatus.FREE);

        slotService.create(userId, new CreateSlotRequest(at("09:00"), 60, null));
        slotService.create(userId, new CreateSlotRequest(at("11:00"), 60, null));

        assertThat(slotRepository.count()).isEqualTo(3);
    }

    @Test
    void mergeSlots() {
        saveSlot(calendar, "09:00", "10:00", SlotStatus.FREE);
        saveSlot(calendar, "10:00", "11:00", SlotStatus.FREE);
        saveSlot(calendar, "11:00", "12:00", SlotStatus.BOOKED);
        saveSlot(calendar, "12:00", "13:00", SlotStatus.FREE);
        saveSlot(calendar, "13:00", "14:00", SlotStatus.BOOKED);
        saveSlot(calendar, "14:00", "15:00", SlotStatus.BOOKED);

        UserSlotStatus result = slotService.availiableSlots(userId, at("09:00"), at("15:00"));

        assertThat(result.from()).isEqualTo(at("09:00"));
        assertThat(result.to()).isEqualTo(at("15:00"));
        assertThat(result.periods().get(SlotStatus.FREE))
                .containsExactly(period("09:00", "11:00"), period("12:00", "13:00"));
        assertThat(result.periods().get(SlotStatus.BOOKED))
                .containsExactly(period("11:00", "12:00"), period("13:00", "15:00"));
    }

    @Test
    void findReturnsSlotsOverlappingRangeOrderedByStartTime() {
        Slot overlapsEnd = saveSlot(calendar, "11:45", "12:30", SlotStatus.FREE);
        Slot overlapsStart = saveSlot(calendar, "09:30", "10:30", SlotStatus.FREE);
        saveSlot(calendar, "12:00", "13:00", SlotStatus.FREE);
        saveSlot(calendar, "09:00", "10:00", SlotStatus.FREE);
        Slot inside = saveSlot(calendar, "11:00", "11:30", SlotStatus.BOOKED);

        Page<SlotResponse> page = slotService.find(userId, at("10:00"), at("12:00"), null, Pageable.unpaged());

        assertThat(page.getContent()).extracting(SlotResponse::id)
                .containsExactly(overlapsStart.getId(), inside.getId(), overlapsEnd.getId());
        assertThat(page.getContent().getFirst()).isEqualTo(
                new SlotResponse(overlapsStart.getId(), at("09:30"), at("10:30"), SlotStatus.FREE));
    }

    @Test
    void findFiltersByStatus() {
        Slot free1 = saveSlot(calendar, "09:00", "10:00", SlotStatus.FREE);
        Slot booked = saveSlot(calendar, "10:00", "11:00", SlotStatus.BOOKED);
        Slot free2 = saveSlot(calendar, "11:00", "12:00", SlotStatus.FREE);

        assertThat(findIds(SlotStatus.FREE)).containsExactly(free1.getId(), free2.getId());
        assertThat(findIds(SlotStatus.BOOKED)).containsExactly(booked.getId());
        assertThat(findIds(null)).containsExactly(free1.getId(), booked.getId(), free2.getId());
    }

    @Test
    void findReturnsOnlySlotsOfRequestedUser() {
        Slot own = saveSlot(calendar, "09:00", "10:00", SlotStatus.FREE);
        saveSlot(createCalendar("test@gmail.com"), "09:00", "10:00", SlotStatus.FREE);

        assertThat(findIds(null)).containsExactly(own.getId());
    }

    @Test
    void findPaginatesResults() {
        List<Slot> slots = IntStream.range(8, 13)
                .mapToObj(hour -> saveSlot(calendar, "%02d:00".formatted(hour), "%02d:00".formatted(hour + 1), SlotStatus.FREE))
                .toList();

        Page<SlotResponse> page = slotService.find(userId, at("00:00"), at("23:59"), null, PageRequest.of(1, 2));

        assertThat(page.getContent()).extracting(SlotResponse::id)
                .containsExactly(slots.get(2).getId(), slots.get(3).getId());
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }

    @Test
    void findRejectsUnknownUser() {
        assertThatThrownBy(() -> slotService.find(UNKNOWN_USER_ID, at("00:00"), at("23:59"), null, Pageable.unpaged()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void availableSlotsClipsSlotsToRequestedRange() {
        saveSlot(calendar, "08:00", "10:00", SlotStatus.FREE);
        saveSlot(calendar, "11:00", "13:00", SlotStatus.BOOKED);

        UserSlotStatus result = slotService.availiableSlots(userId, at("09:00"), at("12:00"));

        assertThat(result.periods().get(SlotStatus.FREE)).containsExactly(period("09:00", "10:00"));
        assertThat(result.periods().get(SlotStatus.BOOKED)).containsExactly(period("11:00", "12:00"));
    }

    @Test
    void availableSlotsReturnsEmptyPeriodsForEveryStatusWhenCalendarIsEmpty() {
        saveSlot(createCalendar("test@gmail.com"), "09:00", "10:00", SlotStatus.FREE);

        UserSlotStatus result = slotService.availiableSlots(userId, at("00:00"), at("23:59"));

        assertThat(result.periods()).containsOnlyKeys(SlotStatus.values());
        assertThat(result.periods().values()).allSatisfy(periods -> assertThat(periods).isEmpty());
    }

    @Test
    void availableSlotsRejectsUnknownUser() {
        assertThatThrownBy(() -> slotService.availiableSlots(UNKNOWN_USER_ID, at("00:00"), at("23:59")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    private List<Long> findIds(SlotStatus status) {
        return slotService.find(userId, at("00:00"), at("23:59"), status, Pageable.unpaged())
                .map(SlotResponse::id)
                .getContent();
    }

    private Calendar createCalendar(String email) {
        User user = new User();
        user.setEmail(email);
        Calendar newCalendar = new Calendar();
        newCalendar.setUser(userRepository.save(user));
        return calendarRepository.save(newCalendar);
    }

    private Slot saveSlot(Calendar owner, String start, String end, SlotStatus status) {
        Slot slot = new Slot();
        slot.setCalendar(owner);
        slot.setStartTime(at(start));
        slot.setEndTime(at(end));
        slot.setStatus(status);
        return slotRepository.save(slot);
    }
}
