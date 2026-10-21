package eva.com.moodle.user;

import eva.com.moodle.common.BusinessException;
import eva.com.moodle.slot.Calendar;
import eva.com.moodle.slot.CalendarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository users;
    private final CalendarRepository calendars;

    public UserResponse create(final CreateUserRequest request) {
        final User userEntity = new User();
        userEntity.setEmail(request.email());
        final User user = users.saveAndFlush(userEntity);
        final Calendar calendar = new Calendar();
        calendar.setUser(user);
        calendars.save(calendar);
        return new UserResponse(user.getId(), user.getEmail());
    }

    @Transactional(readOnly = true)
    public UserResponse get(final Long userId) {
        return users.findById(userId)
                .map(u -> new UserResponse(u.getId(), u.getEmail()))
                .orElseThrow(() -> BusinessException.notFound("User", userId));
    }
}
