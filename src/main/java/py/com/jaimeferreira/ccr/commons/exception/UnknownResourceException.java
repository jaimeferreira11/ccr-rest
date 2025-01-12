package py.com.jaimeferreira.ccr.commons.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by mcespedes
 *
 * Simulated business-logic exception indicating a desired business entity or record cannot be found.
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class UnknownResourceException extends RuntimeException {

    public UnknownResourceException(String msg) {
        super(msg);
    }
}