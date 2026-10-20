package eva.com.moodle.slot;

import java.time.OffsetDateTime;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SlotService {

    @Transactional
    public SlotResponse create(Long userId, CreateSlotRequest request) {
        throw new NotImplementedException();
    }

    @Transactional
    public Page<SlotResponse> find(Long userId, OffsetDateTime from, OffsetDateTime to, SlotStatus status, Pageable pageable) {
        throw new NotImplementedException();
    }
}
