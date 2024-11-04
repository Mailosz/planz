package pl.mo.planz.view;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingSupplier;

import pl.mo.planz.StringUtils;
import pl.mo.planz.TemplateParsingException;
import pl.mo.planz.dto.TemplateDTO;
import pl.mo.planz.model.DocumentModel;
import pl.mo.planz.model.FieldType;
import pl.mo.planz.model.FieldValueModel;
import pl.mo.planz.model.SeriesModel;
import pl.mo.planz.model.TemplateFieldModel;
import pl.mo.planz.model.TemplateModel;
import pl.mo.planz.model.DatalistModel;
import pl.mo.planz.repositories.TemplateRepository;
import pl.mo.planz.templates.TemplateParser;
import pl.mo.planz.utils.Lazy;

@Component
public class PageBuilder {

    static String htmlEnd = "</body></html>";

    static String htmlStartUrl = "page/htmlStart.html";
    static String editScriptUrl = "page/edit.script.js";
    static String adminScriptUrl = "page/adminScript.html";

    static Map<String,String> resources = new HashMap<String,String>();

    static Lazy<TemplateDTO> viewPageTemplate = new Lazy<TemplateDTO>(() -> {
        return loadPageTemplate("page/page.view.html");
    });

    static Lazy<TemplateDTO> editPageTemplate = new Lazy<TemplateDTO>(() -> {
        return loadPageTemplate("page/edit.view.html");
    });    

    static Lazy<TemplateDTO> editScriptTemplate = new Lazy<TemplateDTO>(() -> {
        return loadPageTemplate("page/edit.script.js");
    });   
    
    static Lazy<TemplateDTO> createPageTemplate = new Lazy<TemplateDTO>(() -> {
        return loadPageTemplate("page/create.view.html");
    });


    public static TemplateDTO loadPageTemplate(String resource) {
        TemplateParser tp = new TemplateParser();

        var templateString = loadResource(resource);

        TemplateDTO template;
        try {
            template = tp.parse(templateString);
        } catch (TemplateParsingException ex) {
            throw new RuntimeException(ex);
        }
        
        return template;
    }
    
    public static String loadResource(String resource) {

        InputStream inputStream = PageBuilder.class.getClassLoader().getResourceAsStream(resource);

        StringBuilder tb = new StringBuilder(10000);
        try (Reader reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                tb.append((char) c);
            }

            return tb.toString();
        } catch (IOException e) {
            System.out.println("Error loading resource: " + resource);
            e.printStackTrace();

            return null;
        }
        
    }

    @Autowired
    TemplateRepository templateRepository;


    private String buildArrows(String prev, String next) {

        StringBuilder sb = new StringBuilder();
        if (prev != null) {
            sb.append("<a id=\"prev-button\" href=\"" + prev + "\"><div class=\"central\">&#10094;</div></a>");;
        }

        if (next != null) {
            sb.append("<a id=\"next-button\" href=\"" + next + "\"><div class=\"central\">&#10095;</div></a>");
        } 
        return sb.toString();
    }

    private String readResource(String url) {
        if (resources.containsKey(url)) {
            return resources.get(url);
        } else {
            var resource = loadResource(url);
            resources.put(url, resource);
            return resource;
        }
    }


    public  String buildCreatePage(SeriesModel series, String token) {


        String result = buildView(createPageTemplate.get(), Map.of(
                "arrows", () -> {
                    if (series.getLastDocument() != null) {
                        return buildArrows("/edit/" + token + "/" + series.getLastDocument().getId(), null);
                    } else {
                        return "";
                    }
                },
                "title", () -> series.getName(),
                "series_id", () -> series.getId().toString(),
                "token", () -> token
            ));

        return result;
    }

    private String getDocumentAddress(String token, DocumentModel doc) {
        return token + "/" + doc.getId().toString();
    }

    public String buildPage(boolean isAdmin, boolean isEdit, DocumentModel doc, String token, boolean containsChanges, String content) {
        
        String result;
        if (isAdmin || isEdit) {
            result = buildView(editPageTemplate.get(), Map.of(
                "content", () -> content,
                "public_checkbox_checked", () -> doc.isPublic() ? "checked" : "",
                "edit_checkbox_checked", () -> doc.isEditable() ? "checked" : "",
                "templates_list", () -> showTemplatesList(doc),
                "doc_contains_changes", () -> containsChanges ? " doc-contains-changes" : "",
                "admin_script", () -> isAdmin ? readResource(adminScriptUrl)  : "",
                "edit_script", () -> {
                    var editScript = buildView(editScriptTemplate.get(), Map.of("token", () -> token, "document_id", () -> doc.getId().toString()));
                    return "<script>" + editScript + "</script>";
                },
                "token_input", () -> hiddenInput("token-input", token),
                "arrows", () -> {
                    String next = null;
                    String prev = null;

                    if (doc.getNext() != null) {
                        next = "/edit/" + getDocumentAddress(token, doc.getNext());
                    } else { // link to create page
                        next = "/create/" + token;
                    }
        
                    if (doc.getPrev() != null) {
                        prev = "/edit/" + getDocumentAddress(token, doc.getPrev());
                    }
                    return buildArrows(prev, next);
                }
            ));
        } else {
            result = buildView(viewPageTemplate.get(), Map.of(
                "content", () -> content,
                "goto_current_week", () -> {
                    var now = LocalDate.now();
                    if (now.isBefore(doc.getDate()) || now.isAfter(doc.getDate().plusWeeks(1))) {
                        return "<a href=\".?token=" + token + "\" class=\"goto-current-week\"></a>";
                    } else {
                        return "";
                    }
                },
                "arrows", () -> {
                    String next = null;
                    String prev = null;
                    if (doc.getNext() != null && doc.getNext().isPublic()) {
                        next = "/view/" + getDocumentAddress(token, doc.getNext());
                    }
        
                    if (doc.getPrev() != null && doc.getPrev().isPublic()) {
                        prev = "/view/" + getDocumentAddress(token, doc.getPrev());
                    }
                    return buildArrows(prev, next);
                }
            ));
        }


        return result;
    }

    private String showTemplatesList(DocumentModel doc) {
        List<TemplateModel> templates = templateRepository.findTemplatesForSeries(doc.getSeries().getId());
            String content = "<label>Szablon: <select onchange=\"templateChange(event)\">";
            for (var template : templates) {
                content += "<option" + (template == doc.getTemplate()?" selected":"") + " value=\"" + template.getId().toString() + "\">" + (StringUtils.isNullOrEmpty(template.getName())?template.getId().toString():template.getName()) + "</option>";
            }
            content += "</select></label>&emsp;";
            return content;
    }

    static String hiddenInput(String id, String value) {
        return "<input type=\"hidden\" id=" + id + " value=\"" + value + "\">";
    }

    public static Pair<String, Boolean> buildDocumentForEdit(DocumentModel docModel, Map<String, FieldValueModel> valueMap, Set<String> userPermissions, String token) {

        String helpInputs = hiddenInput("token-input", token) + hiddenInput("document-id-input", docModel.getId().toString());

        if (docModel.getGeneratedTime() != null) {
            helpInputs += "<input type=\"hidden\" id=\"document-gen-time\" value=\"" + docModel.getGeneratedTime().toString() + "\";>";
        }

        List<TemplateFieldModel> fields = docModel.getTemplate().getFields();
        fields.sort((f1, f2) -> Integer.compare(f2.getPos(), f1.getPos()));

        boolean isAdmin = userPermissions.contains("admin");
        boolean anythingChanged = false;

        //building template
        String template = docModel.getTemplate().getContent();
        StringBuffer templateBuffer = new StringBuffer(template);
        Set<DatalistModel> datalists = new HashSet<>();
        for (var field : fields) {

            String listname;
            if (field.getDatalist() == null) {
                listname = "";
            } else {
                datalists.add(field.getDatalist());
                listname = "list=\"" + field.getDatalist().getName() + "\"";
            }

            if (!isAdmin && (field.getEditPermission() == null || !userPermissions.contains(field.getEditPermission().getName()))) { // just value

                String staticValue = getStaticValue(field, docModel, valueMap);

                templateBuffer.insert(field.getPos(), staticValue);
            } else { // edit
                var valueModel = valueMap.get(field.getName());

                String input = getEditValue(field, docModel, valueModel, valueMap, listname);
                
                if (isAdmin) {
                    boolean changed = false;
                    if (valueModel != null && docModel.getGeneratedTime() != null && valueModel.getEditTime() != null && docModel.getGeneratedTime().isBefore(valueModel.getEditTime())) {
                        changed = true;
                        anythingChanged = true;
                    }
                    input = "<div class=\"edit-hud admin" + (changed?" changed":"")+ "\" >" + input + "<div class='more' onclick=\"showAdminPopup('" + field.getId().toString() + "', '" + (valueModel!= null?valueModel.getId():"") + "', event)\">...</div></div>";
                } else {
                    input = "<div class=\"edit-hud\" >" + input + "</div>";
                }

                templateBuffer.insert(field.getPos(), input);
            }
        }

        //datalists
        StringBuilder sb = new StringBuilder(10000);
        for (var datalist : datalists) {
            if (datalist != null) {
                sb.append("<datalist id=\"" + datalist.getName() + "\">");

                for (var item : datalist.getValues()) {
                    sb.append("<option value=\"" + StringUtils.escapeQuotes(item.getValue()) + "\">");
                }
                sb.append("</datalist>");
            }
        }

        return Pair.of(helpInputs + sb.toString() + templateBuffer.toString(), anythingChanged);
    }

    private static String getEditValue(TemplateFieldModel field, DocumentModel docModel, FieldValueModel valueModel, Map<String, FieldValueModel> valueMap, String listname) {

        String value = null;
        FieldType type = null;
        if (valueModel != null) {
            if (valueModel.getValue() != null) {
                value = valueModel.getValue();
            }
            if (valueModel.getType() != null) {
                type = valueModel.getType();
            }
        }

        if (value == null) {
            value = field.getDefaultValue();
        }

        if (type == null) {
            type = field.getType();
        }

        switch (type) {
            case AUTO:
                return getFieldAutoValue(docModel, value);
            case COPY:
                return getFieldCopyValue(value, valueMap);
            case HIDDEN:
                return getStaticValue(field, docModel, valueMap);
            default:
                return String.format("<input type=\"text\" id=\"%1$s\" name=\"%2$s\" %3$s class=\"user-editable-field\" value=\"%4$s\" oninput=\"fieldInput(event)\" onchange=\"fieldChange(event)\">", field.getId(), field.getName(), listname, StringUtils.escapeHTML(StringUtils.escapeQuotes(value)));
        }
    }

    private static String getStaticValue(TemplateFieldModel field, DocumentModel docModel, Map<String, FieldValueModel> valueMap) {

        var valueModel = valueMap.get(field.getName());
        String value = null;
        FieldType type = null;
        if (valueModel != null) {
            if (valueModel.getValue() != null) {
                value = valueModel.getValue();
            }
            if (valueModel.getType() != null) {
                type = valueModel.getType();
            }
        }

        if (value == null) {
            value = field.getDefaultValue();
        }

        if (type == null) {
            type = field.getType();
        }

        switch (type) {
            case AUTO:
                return getFieldAutoValue(docModel, value);
            case COPY:
                return getFieldCopyValue(value, valueMap);
            case TEXT:
                return StringUtils.escapeHTML(value);
            default:
                return value;
        }
    }

    public static String buildView(TemplateDTO template, Map<String, Supplier<String>> valueMap) {

        template.getFields().sort((f1, f2) -> Integer.compare(f2.getPos(), f1.getPos()));

        StringBuffer templateBuffer = new StringBuffer(template.getContent());
        for (var field : template.getFields()) {

            String staticValue = valueMap.get(field.getName()).get();

            templateBuffer.insert(field.getPos(), staticValue);
        }

        return templateBuffer.toString();
    }

    public static String buildTemplateForView(DocumentModel docModel, Map<String, FieldValueModel> valueMap) {

        List<TemplateFieldModel> fields = docModel.getTemplate().getFields();
        fields.sort((f1, f2) -> Integer.compare(f2.getPos(), f1.getPos()));

        String template = docModel.getTemplate().getContent();
        StringBuffer templateBuffer = new StringBuffer(template);
        for (var field : fields) {

            String staticValue = getStaticValue(field, docModel, valueMap);

            templateBuffer.insert(field.getPos(), staticValue);
        }

        return templateBuffer.toString();
    }

    static DateTimeFormatter dateFormattter = DateTimeFormatter.ofPattern("dd.MM.YYYY");

    private static String getFieldAutoValue(DocumentModel doc, String name) {
        
        return switch (name) {
            case "tydzien-od" -> doc.getDate().getDayOfMonth() + " " + switch (doc.getDate().getMonthValue()) {
                case 1 -> "stycznia";
                case 2 -> "lutego";
                case 3 -> "marca";
                case 4 -> "kwietnia";
                case 5 -> "maja";
                case 6 -> "czerwca";
                case 7 -> "lipca";
                case 8 -> "sierpnia";
                case 9 -> "września";
                case 10 -> "października";
                case 11 -> "listopada";
                case 12 -> "grudnia";
                default -> "";
            };
            case "poniedziałek" -> doc.getDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)).format(dateFormattter);
            case "wtorek" -> doc.getDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY)).format(dateFormattter);
            case "środa" -> doc.getDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY)).format(dateFormattter);
            case "czwartek" -> doc.getDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY)).format(dateFormattter);
            case "piątek" -> doc.getDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY)).format(dateFormattter);
            case "sobota" -> doc.getDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)).format(dateFormattter);
            case "niedziela" -> doc.getDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).format(dateFormattter);
            default -> "";
        };
    }

    private static String getFieldCopyValue(String value, Map<String, FieldValueModel> valueMap) {
        var valueModel = valueMap.get(value);
        if (valueModel == null) {
            return "";
        } else {
            return valueModel.getValue();
        }
    }
}
