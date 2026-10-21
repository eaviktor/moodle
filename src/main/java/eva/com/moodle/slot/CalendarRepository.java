package eva.com.moodle.slot;

import eva.com.moodle.common.BusinessException;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface CalendarRepository extends JpaRepository<Calendar, Long> {

    Optional<Calendar> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Calendar> findForUpdateByUserId(Long userId);

    default Calendar getByUserId(final Long userId) {
        return findByUserId(userId).orElseThrow(() -> BusinessException.notFound("User", userId));
    }

    default Calendar lockByUserId(final Long userId) {
        return findForUpdateByUserId(userId).orElseThrow(() -> BusinessException.notFound("User", userId));
    }
}
