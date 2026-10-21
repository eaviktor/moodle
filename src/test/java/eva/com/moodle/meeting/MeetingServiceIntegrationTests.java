package eva.com.moodle.meeting;

import eva.com.moodle.TestcontainersConfiguration;
import eva.com.moodle.common.BusinessException;
import eva.com.moodle.slot.*;
import eva.com.moodle.user.User;
import eva.com.moodle.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MeetingServiceIntegrationTests {

    private static final List<Participant> TEST_LIST = List.of(
            new Participant("Test1", "test1@gmail.com"),
            new Participant("Test2", null));

    @Autowired
    private MeetingService meetingService;
    @Autowired
    private MeetingRepository meetingRepository;
    @Autowired
    private SlotRepository slotRepository;
    @Autowired
    private CalendarRepository calendarRepository;
    @Autowired
    private UserRepository userRepository;

    private Calendar calendar;
    private Long userId;

    private static OffsetDateTime at(String time) {
        return OffsetDateTime.of(2026, 1, 5, 0, 0, 0, 0, ZoneOffset.UTC).with(LocalTime.parse(time));
    }

    private static ScheduleMeetingRequest planSlot(Slot slot) {
        return new ScheduleMeetingRequest(slot.getId(), "title", "description", TEST_LIST);
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
    void scheduleMeeting() {
        Slot testSlot = saveSlot(calendar, SlotStatus.FREE);

        MeetingResponse response = meetingService.schedule(userId, planSlot(testSlot));

        assertThat(response).isEqualTo(new MeetingResponse(response.id(), testSlot.getId(), at("09:00"), at("10:00"),
                "title", "description", TEST_LIST));
        assertThat(slotRepository.findById(testSlot.getId())).get().extracting(Slot::getStatus).isEqualTo(SlotStatus.BOOKED);
        assertThat(meetingService.get(userId, response.id())).isEqualTo(response);
    }

    @Test
    void rejectsIfSLotNotFree() {
        Slot testSlot = saveSlot(calendar, SlotStatus.BOOKED);

        assertThatThrownBy(() -> meetingService.schedule(userId, planSlot(testSlot)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);
        assertThat(meetingRepository.count()).isZero();
    }

    @Test
    void scheduleRejectsSlotOfAnotherUser() {
        Slot testSlot = saveSlot(createCalendar("test@gmail.com"), SlotStatus.FREE);

        assertThatThrownBy(() -> meetingService.schedule(userId, planSlot(testSlot)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
        assertThat(slotRepository.findById(testSlot.getId())).get().extracting(Slot::getStatus).isEqualTo(SlotStatus.FREE);
    }

    private Calendar createCalendar(String email) {
        User user = new User();
        user.setName(email);
        user.setEmail(email);
        Calendar newCalendar = new Calendar();
        newCalendar.setUser(userRepository.save(user));
        return calendarRepository.save(newCalendar);
    }

    private Slot saveSlot(Calendar owner, SlotStatus status) {
        Slot slot = new Slot();
        slot.setCalendar(owner);
        slot.setStartTime(at("09:00"));
        slot.setEndTime(at("10:00"));
        slot.setStatus(status);
        return slotRepository.save(slot);
    }
}

