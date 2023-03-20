package pl.mo.planz;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pl.mo.planz.dto.DocumentDTO;
import pl.mo.planz.dto.DocumentResponseDTO;
import pl.mo.planz.dto.TemplateFieldDTO;
import pl.mo.planz.model.DocumentModel;
import pl.mo.planz.model.FieldValueHistoryModel;
import pl.mo.planz.model.FieldValueModel;
import pl.mo.planz.model.IdentityModel;
import pl.mo.planz.model.TemplateFieldModel;
import pl.mo.planz.model.TemplateModel;
import pl.mo.planz.model.ValueListModel;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;


@RestController
@CrossOrigin
public class Controller {
    
    @Autowired
    TemplateRepository templateRepository;

    @Autowired
    DocumentRepository documentRepository;

    @Autowired
    FieldValueRepository fieldValueRepository;

    @Autowired
    ProfileRepository profileRepository;

    @Autowired
    ValueListRepository listRepository;

    @Autowired
    FieldRepository fieldRepository;

    @Autowired
    IdentityRepository identityRepository;

    @Autowired
    FieldValueHistoryRepository historyRepository;

    public void parseTemplateAndSave(String template, TemplateModel tm) throws TemplateParsingException {
        TemplateParser tp = new TemplateParser(template);
        template = tp.parse();
        
        tm.setContent(template);

        List<TemplateFieldModel> oldFields = tm.getFields();
        if (oldFields == null) oldFields = new ArrayList<>();
        List<TemplateFieldDTO> foundFields = tp.getFields();
        List<TemplateFieldModel> newFields = new ArrayList<>(foundFields.size());
        for (var field : foundFields) {
            TemplateFieldModel tfm;
            if (field.getName() != null) {
                Optional<TemplateFieldModel> oldOpt = oldFields.stream().filter((f) -> field.getName().equals(f.getName())).findFirst();
                if (oldOpt.isPresent()) {
                    tfm = oldOpt.get();
                    oldFields.remove(oldOpt.get());
                } else {
                    tfm = new TemplateFieldModel();
                }
            } else {
                tfm = new TemplateFieldModel();
            }

            tfm.setTemplate(tm);
            tfm.setAutoMethod(field.getAuto());
            tfm.setName(field.getName());
            tfm.setPos(field.getPos());
            tfm.setDefaultValue(Optional.ofNullable(field.getDefaultValue()).orElse(""));
            tfm.setPublic(Optional.ofNullable(field.getIsPublic()).orElse(false));

            if (field.getEdit() != null) {
                var opt = profileRepository.findByName(field.getEdit());
                if (opt.isPresent()) {
                    tfm.setEditProfile(opt.get());
                } else {
                    throw new TemplateParsingException("No profile");
                }
            }

            if (field.getList() != null) {
                var opt = listRepository.findByName(field.getList());
                if (opt.isPresent()) {
                    tfm.setDatalist(opt.get());
                } else {
                    throw new TemplateParsingException("No list");
                }
            }

            newFields.add(tfm);
        }

        tm.setFields(newFields);
        templateRepository.save(tm);
    }

    @PostMapping(value="templates")
    public String postTemplate(@RequestBody String template, @RequestParam(name = "token", required = false) String token) {

        //"security"
        adminOrThrow(token);
        
        TemplateModel tm = new TemplateModel();

        try {
            parseTemplateAndSave(template, tm);
        } catch (TemplateParsingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Parsing error: " + e.getDesc());
        }
        
        return tm.getId().toString();
    }

    @PutMapping(value="templates/{uuid}")
    public String updateTemplate(@PathVariable("uuid") UUID id, @RequestParam(name = "token", required = false) String token, @RequestBody String content) {
        
        //"security"
        adminOrThrow(token);

        var templateOpt = templateRepository.findById(id);

        if (templateOpt.isPresent()) {
            try {
                parseTemplateAndSave(content, templateOpt.get());
            } catch (TemplateParsingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Parsing error: " + e.getDesc());
            }
            return "ok";
        } else {
            return "no template";
        }
    }

    @GetMapping(value="templates")
    public List<UUID> getTemplates(@RequestParam(name = "token", required = false) String token) {
        
        //"security"
        adminOrThrow(token);

        List<UUID> ids =templateRepository.getAllIds();
        
        return ids;
    }

    @GetMapping(value="templates/{uuid}")
    public String getTemplate(@PathVariable("uuid") UUID id, @RequestParam(name = "token", required = false) String token) {

        //"security"
        adminOrThrow(token);
        
        var templateOpt = templateRepository.findById(id);

        if (templateOpt.isPresent()) {
            return templateOpt.get().getContent();
        } else {
            return "no template";
        }
        
    }

    public void adminOrThrow(String token) {
        var identity = identityRepository.findFromToken(token);
        Set<String> profiles = getProfiles(identity);

        if (!profiles.contains("admin")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping(value="documents")
    public UUID postDocument(@RequestBody DocumentDTO dto, @RequestParam(name = "token", required = false) String token) {

        //"security"
        adminOrThrow(token);


        DocumentModel doc = new DocumentModel();

        doc.setWeek(dto.getWeek());
        doc.setTemplate(templateRepository.getReferenceById(dto.getTemplateId()));

        documentRepository.save(doc);
        
        return doc.getId();
    }

    @GetMapping(value="documents/{uuid}/change/public/{public}")
    public String changeIsPublic(@PathVariable("uuid") UUID id, @RequestParam(name = "token", required = false) String token, @PathVariable("public") Boolean isPublic) {

        //"security"
        adminOrThrow(token);

        var opt = documentRepository.findById(id);

        if (opt.isPresent()) {
            opt.get().setPublic(Optional.of(isPublic).get());
            documentRepository.save(opt.get());
        }

        return Boolean.toString(opt.get().isPublic());
    }

    @GetMapping(value="documents/{uuid}/change/editable/{editable}")
    public String changeIsEditable(@PathVariable("uuid") UUID id, @RequestParam(name = "token", required = false) String token, @PathVariable("editable") Boolean iseditable) {

        //"security"
        adminOrThrow(token);

        var opt = documentRepository.findById(id);

        if (opt.isPresent()) {
            opt.get().setEditable(Optional.of(iseditable).get());
            documentRepository.save(opt.get());
        }

        return Boolean.toString(opt.get().isEditable());
    }

    @PostMapping(value="documents/{uuid}")
    public String updateDocument(@PathVariable("uuid") UUID id, @RequestBody DocumentDTO dto, @RequestParam(name = "token", required = false) String token) {

        //"security"
        adminOrThrow(token);

        var opt = documentRepository.findById(id);

        if (opt.isPresent()) {
            var model = opt.get();
            model.setTemplate(templateRepository.getReferenceById(dto.getTemplateId()));
            model.setWeek(dto.getWeek());
            documentRepository.save(model);
            return "ok";
        } else {
            return "no template";
        }
    }

    @GetMapping(value="/")
    public String openCurrentDocument(@RequestParam(name = "token", required = false) String token) {

        if (token == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        var opt = documentRepository.findCurrentForDate(LocalDate.now());
        
        DocumentModel doc = null;
        if (opt.isPresent()) {
            doc = opt.get();
        } else {
            var docs = documentRepository.findAll();
            if (docs.size() > 0) {
                doc = docs.get(0);
            }
        }

        if (doc == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return createView(doc, token);
    }

    @GetMapping(value="/{docId}")
    public String openDocument(@PathVariable("docId") UUID docId, @RequestParam(name = "token", required = false) String token, HttpServletResponse response) {

        if (token == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        var opt = documentRepository.findById(docId);
        
        if (opt.isPresent()) {
            return createView(opt.get(), token);
        } else {
            response.setHeader("Location", "/?token=" + token);
            response.setStatus(307);
            return "";
        }
    }

    public String createView(DocumentModel doc, String token) {

        var identity = identityRepository.findFromToken(token);
        Set<String> profiles = getProfiles(identity);

        boolean isAdmin = false;
        if (profiles.contains("admin")) {
            isAdmin = true;
        }

        boolean isEdit = false;
        if (profiles.contains("edit")) {
            isEdit = true;
        }

        String next = null;
        String prev = null;
        if (isAdmin || isEdit) {
            if (doc.getNext() != null) {
                next = doc.getNext().getId().toString() + "?token=" + token;
            }

            if (doc.getPrev() != null) {
                prev = doc.getPrev().getId().toString() + "?token=" + token;
            }
        } else {
            if (doc.getNext() != null && doc.getNext().isPublic()) {
                next = doc.getNext().getId().toString() + "?token=" + token;;
            }

            if (doc.getPrev() != null && doc.getPrev().isPublic()) {
                prev = doc.getPrev().getId().toString() + "?token=" + token;;
            }
        }

        String content = "";
        if (isAdmin || (isEdit && doc.isEditable())) {
            Map<UUID, String> valueMap = fieldValueRepository.getAllForDocumentId(doc.getId()).stream().collect(Collectors.toMap((fv) -> fv.getField().getId(), (fv) -> fv.getValue()));
        
            content = PageBuilder.buildDocumentForEdit(doc, valueMap, profiles, token);
        } else {
            if (doc.isPublic() || isEdit) {

                if (doc.getGeneratedContent() == null) {

                    Map<UUID, String> valueMap = fieldValueRepository.getAllForDocumentId(doc.getId()).stream().collect(Collectors.toMap((fv) -> fv.getField().getId(), (fv) -> fv.getValue()));
                    content = PageBuilder.buildTemplateForView(doc, valueMap);

                    doc.setGeneratedContent(content);
                    documentRepository.save(doc);

                } else {
                    content = doc.getGeneratedContent();
                }
            } else {
                content = "Podgląd niedostępny";
            }

        }


        


        return PageBuilder.buildPage(isAdmin, isEdit, prev, next, content, doc, token);
    }

    @GetMapping(value="documents")
    public List<DocumentDTO> getDocuments(@RequestParam(name = "token", required = false) String token) {

        //"security"
        adminOrThrow(token);

        List<DocumentModel> models = documentRepository.findAll();

        List<DocumentDTO> dtos = models.stream().map((m) -> docModelToDto(m)).collect(Collectors.toList());
        
        return dtos;
    }



    @PutMapping(value="fields/{docId}/{fieldId}")
    @ResponseStatus(code = HttpStatus.OK)
    public void updateFieldValue(@PathVariable("docId") UUID docId, @PathVariable("fieldId") UUID fieldId, @RequestParam("token") String token, @RequestBody String value) {

        var identity = identityRepository.findFromToken(token);
        if (!identity.isPresent()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Set<String> profiles = getProfiles(identity);

        var opt = fieldValueRepository.findByDocAndField(docId, fieldId);
        
        FieldValueModel model;
        if (opt.isPresent()) {
            model = opt.get();

            //saving history of edits
            // FieldValueHistoryModel fvh = new FieldValueHistoryModel();
            // fvh.setField(model);
            // fvh.setValue(model.getValue());
            // fvh.setEditIdentity(model.getEditIdentity());
            // fvh.setEditTime(model.getEditTime());

            // historyRepository.save(fvh);

        } else {
            var doc = documentRepository.findById(docId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No document"));
            var field = fieldRepository.findById(fieldId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No field"));

            model = new FieldValueModel();
            model.setDocument(doc);
            model.setField(field);
        }

        if (!profiles.contains("admin") && (!model.getDocument().isEditable() || model.getField().getEditProfile() == null || !profiles.contains(model.getField().getEditProfile().getName()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        model.setValue(value);
        model.setEditTime(Instant.now());
        model.setEditIdentity(identity.get());

        fieldValueRepository.save(model);

        if (model.getDocument().getGeneratedContent() != null) {
            model.getDocument().setGeneratedContent(null);
            documentRepository.save(model.getDocument());
        }
    }


    private Set<String> getProfiles(Optional<IdentityModel> identity) {
        if (identity.isPresent()) {
            return identity.get().getProfiles().stream().map((p) -> p.getName()).collect(Collectors.toSet());
        } else {
            return new HashSet<String>();
        }
    }

    @GetMapping(value="test") 
    public void test(@RequestParam("token") String token) {
        System.out.println("TOKEN: " + token);
    }


    private DocumentDTO docModelToDto(DocumentModel model) {
        DocumentDTO dto = new DocumentDTO();
        dto.setTemplateId(model.getTemplate().getId());
        dto.setWeek(model.getWeek());
        dto.setId(model.getId());

        return dto;
    }

    private DocumentModel docDtoToModel(DocumentDTO dto) {
        DocumentModel model = new DocumentModel();
        model.setWeek(dto.getWeek());
        model.setTemplate(templateRepository.getReferenceById(dto.getTemplateId()));

        return model;
    }
}
