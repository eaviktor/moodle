package eva.com.moodle.slot;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;

public interface SlotRepository extends JpaRepository<Slot, Long> {

    Optional<Slot> findByIdAndCalendar(Long id, Calendar calendar);

    @Query("""
            select s from Slot s
            where s.calendar = :calendar and s.status in :statuses and s.startTime < :to and s.endTime > :from
            order by s.startTime""")
    Page<Slot> findInRange(Calendar calendar, Collection<SlotStatus> statuses, OffsetDateTime from, OffsetDateTime to, Pageable pageable);
}
