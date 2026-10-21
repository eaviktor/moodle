package eva.com.moodle.meeting;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping("/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public MeetingResponse schedule(@PathVariable final Long userId,
                                    @Valid @RequestBody final ScheduleMeetingRequest request) {
        return meetingService.schedule(userId, request);
    }

    @GetMapping("/{userId}/{meetingId}")
    public MeetingResponse get(@PathVariable final Long userId, @PathVariable final Long meetingId) {
        return meetingService.get(userId, meetingId);
    }
}
