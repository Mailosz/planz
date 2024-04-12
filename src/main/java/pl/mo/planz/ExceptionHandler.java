package pl.mo.planz;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ExceptionHandler implements HandlerExceptionResolver {

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            if (ex instanceof ResponseStatusException) {
                ResponseStatusException rse = (ResponseStatusException)ex;
                if (rse.getMessage() != null && !rse.getMessage().isBlank()) {
                    response.sendError(rse.getStatusCode().value(), rse.getMessage());
                    return new ModelAndView();
                }
            }
        } catch (Exception handlerException) {
            //logger.warn("Handling of [" + ex.getClass().getName() + "] resulted in Exception", handlerException);
        }
        return null;
    }

}
