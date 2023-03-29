package pl.mo.planz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import lombok.Getter;
import pl.mo.planz.dto.TemplateFieldDTO;
import pl.mo.planz.model.FieldType;

public class TemplateParser {
    
    String template;
    @Getter
    List<TemplateFieldDTO> fields;

    public TemplateParser(String template) {
        this.template = template;
    }


    /**
     * Removes fields from a template and updates fields list
     * @param template Template's string
     * @param fields Fields list to be updated (fields added, removed and changed )
     * @return parsed template with fields removed
     * @throws TemplateParsingException
     */
    public String parse() throws TemplateParsingException {

        fields = new ArrayList<>();

        StringBuilder sb = new StringBuilder(template.length());
        int a = 0;
        int b = 0;
        do  {
            b = template.indexOf("@", a);


            if (b == -1) {
                sb.append(template.substring(a));
                break;
            } else {
                sb.append(template.substring(a, b));

                a = template.indexOf("@", b+1);

                if (a == -1) {
                    sb.append(template.substring(b+1));
                    break;
                } else if (b == a + 1) {
                    sb.append("@"); // @@ zamieniamy na @
                } else {
                    String fieldString = template.substring(b+1, a);

                    TemplateFieldDTO tf = new TemplateFieldDTO();
                    //reading declaration
                    String[] params = fieldString.split(Pattern.quote("|"));
                    for (String param : params) {
                        String[] parts = param.split(Pattern.quote("="));
                        if (parts.length == 1) {
                            if (tf.getName() != null) throw new TemplateParsingException("Duplicate name declaration (name = " + tf.getName() + ")");
                            tf.setName(parts[0]);
                        } else if (parts.length == 2){
                            var name = parts[0].trim();
                            if ("name".equalsIgnoreCase(name)) {
                                if (tf.getName() != null) throw new TemplateParsingException("duplicate name declaration");
                                tf.setName(parts[1].trim());
                            } else if ("list".equalsIgnoreCase(name)) {
                                if (tf.getList() != null) throw new TemplateParsingException("duplicate list declaration");
                                tf.setList(parts[1].trim());
                            } else if ("type".equalsIgnoreCase(name)) {
                                if (tf.getType() != null) throw new TemplateParsingException("duplicate type declaration");
                                String type = parts[1];
                                var opt = Arrays.stream(FieldType.values()).filter((ft) -> ft.name().equalsIgnoreCase(type)).findAny();

                                if (opt.isPresent()) {
                                    tf.setType(opt.get());
                                } else {
                                    throw new TemplateParsingException(String.format("wrong type declaration (type = %1$s)", type));
                                }
                            } else if ("edit".equalsIgnoreCase(name)) {
                                if (tf.getEdit() != null) throw new TemplateParsingException("duplicate edit declaration");
                                tf.setEdit(parts[1].trim());
                            } else if ("default".equalsIgnoreCase(name)) {
                                if (tf.getDefaultValue() != null) throw new TemplateParsingException("duplicate default declaration");
                                tf.setDefaultValue(parts[1].trim());
                            } else if ("public".equalsIgnoreCase(name)) {
                                if (tf.getIsPublic() != null) throw new TemplateParsingException("duplicate public declaration");
                                var v = parts[1].trim();
                                if ("true".equalsIgnoreCase(v)) {
                                    tf.setIsPublic(true);
                                } else if ("false".equalsIgnoreCase(v)) {
                                    tf.setIsPublic(false);
                                } else {
                                    throw new TemplateParsingException("wrong value of 'public' parameter");
                                }
                            }
                        } else {
                            throw new TemplateParsingException();
                        }
                    }
                    
                    if (tf.getName() != null) {
                        if (fields.stream().anyMatch((f) -> tf.getName().equals(f.getName()))) {
                            throw new TemplateParsingException("duplicate named fields (name = " + tf.getName() + ")");
                        }
                    }
                    tf.setPos(sb.length());
                    fields.add(tf);

                }
                a++;
            }

        } while (a < template.length());


        return sb.toString();
    }
}
