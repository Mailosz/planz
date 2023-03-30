package pl.mo.planz.controllers;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pl.mo.planz.PageBuilder;
import pl.mo.planz.StringUtils;
import pl.mo.planz.TemplateParser;
import pl.mo.planz.TemplateParsingException;
import pl.mo.planz.dto.DocumentDTO;
import pl.mo.planz.dto.DocumentResponseDTO;
import pl.mo.planz.dto.FieldDeclarationDTO;
import pl.mo.planz.dto.TemplateFieldDTO;
import pl.mo.planz.model.DocumentModel;
import pl.mo.planz.model.FieldType;
import pl.mo.planz.model.FieldValueHistoryModel;
import pl.mo.planz.model.FieldValueModel;
import pl.mo.planz.model.IdentityModel;
import pl.mo.planz.model.TemplateFieldModel;
import pl.mo.planz.model.TemplateModel;
import pl.mo.planz.model.ValueListModel;
import pl.mo.planz.repositories.DocumentRepository;
import pl.mo.planz.repositories.FieldRepository;
import pl.mo.planz.repositories.FieldValueHistoryRepository;
import pl.mo.planz.repositories.FieldValueRepository;
import pl.mo.planz.repositories.IdentityRepository;
import pl.mo.planz.repositories.ProfileRepository;
import pl.mo.planz.repositories.TemplateRepository;
import pl.mo.planz.repositories.ValueListRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.chrono.ChronoPeriod;
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
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
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

    public static UUID currentDocumentId;
    
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
            tfm.setName(field.getName());
            tfm.setType(Optional.ofNullable(field.getType()).orElse(FieldType.TEXT));
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
    public String postTemplate(@RequestBody String template, @RequestParam(name = "token", required = false) String token, @RequestParam(name = "name", required = false) String name) {

        //"security"
        adminOrThrow(token);
        
        TemplateModel tm = new TemplateModel();
        if (name != null) tm.setName(name);

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
    public String updateTemplate(@PathVariable("uuid") UUID id, @RequestParam(name = "token", required = false) String token, @RequestParam(name = "name", required = false) String name, @RequestBody(required = false) String content) {
        
        //"security"
        adminOrThrow(token);

        var templateOpt = templateRepository.findById(id);

        if (templateOpt.isPresent()) {
            if (name != null) templateOpt.get().setName(name);
            if (!StringUtils.isNullOrEmpty(content)) {
                try {
                    parseTemplateAndSave(content, templateOpt.get());
                } catch (TemplateParsingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Parsing error: " + e.getDesc());
                }
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

    @GetMapping(value="documents/{uuid}/change/template/{templateId}")
    public String changeDocTemplate(@PathVariable("uuid") UUID docId, @RequestParam(name = "token", required = false) String token, @PathVariable("templateId") UUID templateId) {

        //"security"
        adminOrThrow(token);

        var docOpt = documentRepository.findById(docId);
        var temOpt = templateRepository.findById(templateId);

        if (docOpt.isPresent() && temOpt.isPresent()) {
            docOpt.get().setTemplate(temOpt.get());
            documentRepository.save(docOpt.get());
            return "true";
        } else {
            return "false";
        }
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

        //if (token == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        Optional<DocumentModel> opt = getCurrentDocument(documentRepository);
        
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

    private static Optional<DocumentModel> getCurrentDocument(DocumentRepository docRepo) {
        if (currentDocumentId != null) {
            
        } else {
            currentDocumentId = findCurrentDocument(docRepo);
        }

        return docRepo.findById(currentDocumentId);
    }

    private static UUID findCurrentDocument(DocumentRepository docRepo) {
        List<DocumentModel> docs = docRepo.findAll();
        
        var now = LocalDate.now();

        DocumentModel current = null;
        for (var doc : docs) {
            if (now.isBefore(doc.getWeek())) {
                if (current == null || current.getWeek().isAfter(doc.getWeek())) {
                    current = doc;
                }
            } else {
                if (current == null || current.getWeek().isBefore(doc.getWeek())) {
                    current = doc;
                }
            }
        }

        return current.getId();
    }

    @GetMapping(value="/{docId}")
    public String openDocument(@PathVariable("docId") UUID docId, @RequestParam(name = "token", required = false) String token, HttpServletResponse response) {

        //if (token == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        var opt = documentRepository.findById(docId);
        
        if (opt.isPresent()) {
            return createView(opt.get(), token);
        } else {
            if (token != null) {
                response.setHeader("Location", "/?token=" + token);
            } else {
                response.setHeader("Location", "/");
            }
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
            Map<UUID, FieldValueModel> valueMap = fieldValueRepository.getAllForDocumentId(doc.getId()).stream().filter((v) -> v.getValue() != null).collect(Collectors.toMap((fv) -> fv.getField().getId(), (fv) -> fv, (val1, val2) -> {return val2;}));
        
            content = PageBuilder.buildDocumentForEdit(doc, valueMap, profiles, token);
        } else {
            if (doc.isPublic() || isEdit) {

                if (doc.getGeneratedContent() == null) {

                    Map<UUID, FieldValueModel> valueMap = fieldValueRepository.getAllForDocumentId(doc.getId()).stream().filter((v) -> v.getValue() != null).collect(Collectors.toMap((fv) -> fv.getField().getId(), (fv) -> fv, (val1, val2) -> {return val2;}));
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


        return PageBuilder.buildPage(isAdmin, isEdit, prev, next, content, doc, token, templateRepository);
    }

    @GetMapping(value="documents")
    public List<DocumentDTO> getDocuments(@RequestParam(name = "token", required = false) String token) {

        //"security"
        adminOrThrow(token);

        List<DocumentModel> models = documentRepository.findAll();

        List<DocumentDTO> dtos = models.stream().map((m) -> docModelToDto(m)).collect(Collectors.toList());
        
        return dtos;
    }


    private Set<String> getProfiles(Optional<IdentityModel> identity) {
        if (identity.isPresent()) {
            return identity.get().getProfiles().stream().map((p) -> p.getName()).collect(Collectors.toSet());
        } else {
            return new HashSet<String>();
        }
    }

    /**
     * Odświeżanie wszystkiego
     * @param token
     */
    @PostMapping(value="update")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @ResponseStatus(code = HttpStatus.OK)
    public void update(@RequestParam("token") String token) {
        adminOrThrow(token);

        int dayOfWeek = LocalDate.now().getDayOfWeek().getValue() - 1;
        LocalDate weekStart = LocalDate.now().minusDays(dayOfWeek);
        List<LocalDate> weeks = weekStart.datesUntil(weekStart.plusMonths(2), Period.ofWeeks(1)).collect(Collectors.toList());
        LocalDate minimumDate = LocalDate.now().minusMonths(2);

        List<DocumentModel> docs = documentRepository.findAll();

        for (var doc : docs) {
            if (doc.getWeek().isBefore(minimumDate)) {
                if (doc.getNext() != null) doc.getNext().setPrev(null);
                if (doc.getPrev() != null) doc.getPrev().setNext(null);
                documentRepository.delete(doc);
            }
        }

        DocumentModel previous = null;
        for (var week : weeks) {

            var docOpt = docs.stream().filter((d) -> d.getWeek().isEqual(week)).findFirst();
            DocumentModel doc;
            if (docOpt.isPresent()) {

                doc = docOpt.get();

                if (previous != null) {
                    doc.setPrev(previous);
                    previous.setNext(doc);
                }

            } else { // create new document
                doc = new DocumentModel();

                doc.setWeek(week);
                doc.setTemplate(templateRepository.findAll().get(0));

                doc.setPrev(previous);
                if (previous != null) {
                    previous.setNext(doc);
                }
                
                documentRepository.save(doc);
                previous = doc;
            }

            previous = doc;
        };


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
