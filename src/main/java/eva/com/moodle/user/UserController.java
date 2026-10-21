package eva.com.moodle.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody final CreateUserRequest request) {
        return userService.create(request);
    }

    @GetMapping("/{userId}")
    public UserResponse get(@PathVariable final Long userId) {
        return userService.get(userId);
    }
}
