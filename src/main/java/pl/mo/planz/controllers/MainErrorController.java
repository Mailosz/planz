package pl.mo.planz.controllers;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;


@RestController
public class MainErrorController implements ErrorController  {

    @RequestMapping("/error")
    public String handleError() {
        //do something like logging
        return "error";
    }

    // @ExceptionHandler(value = {ResponseStatusException.class})
    // @ResponseBody
    // public String badRequest(final ResponseStatusException ex) {
    //     return "not ok";
    // }

    // @ExceptionHandler(value = {ResponseStatusException.class})
    // @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    // @ResponseBody
    // public String badRequest(final ResponseStatusException ex) {
    //     return "not ok";
    // }
}
