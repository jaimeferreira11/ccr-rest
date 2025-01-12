package py.com.jaimeferreira.ccr.commons.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by rodrigovillalba on 2/16/18.
 * <p>
 * Simple exception with a message, that returns an Internal Server Error code.
 */
@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String msg) {
        super(msg);
    }

    public ForbiddenException(Throwable e) {
        super(e);
    }

}
