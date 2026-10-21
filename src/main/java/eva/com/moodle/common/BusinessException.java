package eva.com.moodle.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(final HttpStatus status, final String message) {
        super(message);
        this.status = status;
    }

    public static BusinessException notFound(final String resource, final Object id) {
        return new BusinessException(HttpStatus.NOT_FOUND, "%s %s not found".formatted(resource, id));
    }

    public static BusinessException conflict(final String message) {
        return new BusinessException(HttpStatus.CONFLICT, message);
    }
}
