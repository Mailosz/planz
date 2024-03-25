package pl.mo.planz.controllers;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pl.mo.planz.DocumentGenerator;
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
import pl.mo.planz.services.IdentityService;

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

    @Autowired
    IdentityService identityService;



    @PostMapping(value="documents")
    public UUID postDocument(@RequestBody DocumentDTO dto, @RequestParam(name = "token", required = false) String token) {

        //"security"
        identityService.adminOrThrow(token);


        DocumentModel doc = new DocumentModel();

        doc.setWeek(dto.getWeek());
        doc.setTemplate(templateRepository.getReferenceById(dto.getTemplateId()));

        documentRepository.save(doc);
        
        return doc.getId();
    }

    @GetMapping(value="documents/{uuid}/change/public/{public}")
    public String changeIsPublic(@PathVariable("uuid") UUID id, @RequestParam(name = "token", required = false) String token, @PathVariable("public") Boolean isPublic) {

        //"security"
        identityService.adminOrThrow(token);

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
        identityService.adminOrThrow(token);

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
        identityService.adminOrThrow(token);

        var docOpt = documentRepository.findById(docId);
        var temOpt = templateRepository.findById(templateId);

        if (docOpt.isPresent() && temOpt.isPresent()) {
            docOpt.get().setTemplate(temOpt.get());
            docOpt.get().setGeneratedContent(null);
            documentRepository.save(docOpt.get());
            return "true";
        } else {
            return "false";
        }
    }

    @PostMapping(value="documents/{uuid}")
    public String updateDocument(@PathVariable("uuid") UUID id, @RequestBody DocumentDTO dto, @RequestParam(name = "token", required = false) String token) {

        //"security"
        identityService.adminOrThrow(token);

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
    public String openCurrentDocument(@RequestParam(name = "token", required = false) String token, @RequestParam(name = "mode", required = false) String mode) {

        // if (token == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        Optional<DocumentModel> opt = getCurrentDocument(documentRepository);
        
        DocumentModel doc = null;
        if (opt.isPresent()) {
            doc = opt.get();
        } else {
            currentDocumentId = null;
            var docs = documentRepository.findAll();
            if (docs.size() > 0) {
                doc = docs.get(0);
            }
        }

        if (doc == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return createView(doc, token, mode);
    }

    private static Optional<DocumentModel> getCurrentDocument(DocumentRepository docRepo) {
        if (currentDocumentId != null) {

        } else {
            currentDocumentId = findCurrentDocument(docRepo);
        }

        return docRepo.findById(currentDocumentId);
    }

    public static UUID findCurrentDocument(DocumentRepository docRepo) {
        List<DocumentModel> docs = docRepo.findAll();
        
        var now = LocalDate.now();

        DocumentModel current = null;
        long diff = 0;
        for (var doc : docs) {
            if (now.isEqual(doc.getWeek()) || (now.isAfter(doc.getWeek()) && now.isBefore(doc.getWeek().plusDays(7)))) {
                //perfect match
                current = doc;
                break;
            } else if (current == null) {
                current = doc;
                diff = Math.abs(ChronoUnit.DAYS.between(now, doc.getWeek()));
            } else {
                long d = Math.abs(ChronoUnit.DAYS.between(now, doc.getWeek()));
                if (d < diff) {
                    current = doc;
                    diff = d;
                }
            }
        }

        return current.getId();
    }

    @GetMapping(value="/{docId}")
    public String openDocument(@PathVariable("docId") UUID docId, @RequestParam(name = "token", required = false) String token, @RequestParam(name = "mode", required = false) String mode, HttpServletResponse response) {

        //if (token == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        var opt = documentRepository.findById(docId);
        
        if (opt.isPresent()) {
            return createView(opt.get(), token, mode);
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

    public String createView(DocumentModel doc, String token, String mode) {

        Set<String> profiles = identityService.getProfilesOrThrow(token);

        boolean isAdmin = false;
        if (profiles.contains("admin")) {
            isAdmin = true;
        }

        boolean isEdit = false;
        if (profiles.contains("edit")) {
            isEdit = true;
        }

        if (isAdmin) {
            if (mode != null) {
                if ("view".equalsIgnoreCase(mode)) {
                    isAdmin = false;
                    isEdit = false;
                } else if ("edit".equalsIgnoreCase(mode)) {
                    isAdmin = false;
                }
            }
        } else if (isEdit) {
            if ("view".equalsIgnoreCase(mode)) {
                isEdit = false;
            }
        } else {
            if (!profiles.contains("view")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
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
        boolean changed = false;
        if (isAdmin || (isEdit && doc.isEditable())) {
            Map<String, FieldValueModel> valueMap = fieldValueRepository.getAllForDocumentId(doc.getId()).stream().filter((v) -> v.getValue() != null).collect(Collectors.toMap((fv) -> fv.getField().getName(), (fv) -> fv, (val1, val2) -> {return val2;}));
        
            var pair = PageBuilder.buildDocumentForEdit(doc, valueMap, profiles, token);
            content = pair.getFirst();
            changed = pair.getSecond();
        } else {
            if (doc.isPublic() || isEdit) {

                if (doc.getGeneratedContent() == null) {
                    content = generateDocumentContent(doc);
                } else {
                    content =  doc.getGeneratedContent();
                }
                
            } else {
                content = "Podgląd niedostępny";
            }

        }


        return PageBuilder.buildPage(isAdmin, isEdit, prev, next, content, doc, token, templateRepository, changed);
    }

    /**
     * Generates document's content
     * @param doc
     * @return Document's generated content
     */
    public String generateDocumentContent(DocumentModel doc) {

        Map<String, FieldValueModel> valueMap = fieldValueRepository.getAllForDocumentId(doc.getId()).stream().filter((v) -> v.getValue() != null).collect(Collectors.toMap((fv) -> fv.getField().getName(), (fv) -> fv, (val1, val2) -> {return val2;}));
        String content = PageBuilder.buildTemplateForView(doc, valueMap);

        doc.setGeneratedContent(content);
        doc.setGeneratedTime(Instant.now());
        documentRepository.save(doc);

        return content;

    }



    @GetMapping(value="documents")
    public List<DocumentDTO> getDocuments(@RequestParam(name = "token", required = false) String token) {

        //"security"
        identityService.adminOrThrow(token);

        List<DocumentModel> models = documentRepository.findAll();

        List<DocumentDTO> dtos = models.stream().map((m) -> docModelToDto(m)).collect(Collectors.toList());
        
        return dtos;
    }


    /**
     * Odświeżanie wszystkiego
     * @param token
     */
    @PostMapping(value="update")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @ResponseStatus(code = HttpStatus.OK)
    public void update(@RequestParam("token") String token) {
        identityService.adminOrThrow(token);

        DocumentGenerator.removeOldDocuments(documentRepository, fieldValueRepository, historyRepository);
        DocumentGenerator.generateDocuments(documentRepository, templateRepository);

    }

    /**
     * Odświeżanie dokumentu
     * @param token
     */
    @PostMapping(value="update/{docId}")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @ResponseStatus(code = HttpStatus.OK)
    public void updateDocumentContent(@PathVariable("docId") UUID docId, @RequestParam(name = "token", required = false) String token) {
        identityService.adminOrThrow(token);

        var docOpt = documentRepository.findById(docId);

        if (docOpt.isPresent()) {
            docOpt.get().setGeneratedContent(generateDocumentContent(docOpt.get()));
            documentRepository.save(docOpt.get());
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

    }


    @GetMapping(value="test") 
    public void test() {
        System.out.println("Here");
        throw new ResponseStatusException(HttpStatus.ALREADY_REPORTED);
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
