package eva.com.moodle.meeting;

import eva.com.moodle.slot.Calendar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    Optional<Meeting> findByIdAndSlotCalendar(Long id, Calendar calendar);

    boolean existsBySlotId(Long slotId);
}
