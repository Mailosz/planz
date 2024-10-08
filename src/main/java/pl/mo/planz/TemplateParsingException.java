package pl.mo.planz;

import lombok.Getter;

public class TemplateParsingException extends Exception {

    @Getter
    String desc;
    public TemplateParsingException() {
        desc = "";
    }

    public TemplateParsingException(String desc) {
        super(desc);
        this.desc = desc;
    }
}
