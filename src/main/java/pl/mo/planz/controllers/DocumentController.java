package pl.mo.planz.controllers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import pl.mo.planz.dto.DocumentDTO;
import pl.mo.planz.model.DocumentModel;
import pl.mo.planz.model.SeriesModel;
import pl.mo.planz.repositories.DocumentRepository;
import pl.mo.planz.repositories.FieldRepository;
import pl.mo.planz.repositories.FieldValueHistoryRepository;
import pl.mo.planz.repositories.FieldValueRepository;
import pl.mo.planz.repositories.IdentityRepository;
import pl.mo.planz.repositories.PermissionRepository;
import pl.mo.planz.repositories.SeriesRepository;
import pl.mo.planz.repositories.TemplateRepository;
import pl.mo.planz.repositories.DatalistRepository;
import pl.mo.planz.services.AccessService;
import pl.mo.planz.services.DocumentService;
import pl.mo.planz.services.IdentityService;

@RestController
@CrossOrigin
public class DocumentController {
    
    @Autowired
    TemplateRepository templateRepository;

    @Autowired
    DocumentRepository documentRepository;

    @Autowired
    FieldValueRepository fieldValueRepository;

    @Autowired
    PermissionRepository profileRepository;

    @Autowired
    DatalistRepository listRepository;

    @Autowired
    FieldRepository fieldRepository;

    @Autowired
    IdentityRepository identityRepository;

    @Autowired
    FieldValueHistoryRepository historyRepository;

    @Autowired
    IdentityService identityService;

    @Autowired
    SeriesRepository seriesRepository;    
    
    @Autowired
    DocumentService documentService;

    @Autowired
    AccessService accessService;


    @GetMapping(value="documents")
    public List<DocumentDTO> getDocuments(@RequestParam(name = "token", required = false) String token) {

        //"security"
        accessService.adminOrThrow(token);

        List<DocumentModel> models = documentRepository.findAll();

        List<DocumentDTO> dtos = models.stream().map((m) -> docModelToDto(m)).collect(Collectors.toList());
        
        return dtos;
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

    @PostMapping(value="documents/create/{seriesId}")
    public UUID postDocument(@PathVariable("seriesId") UUID seriesId, @RequestParam(name = "token", required = false) String token) {

        //"security"
        accessService.adminOrThrow(token);


        Optional<SeriesModel> seriesOpt = seriesRepository.findById(seriesId);

        if (seriesOpt.isPresent()) {
            DocumentModel doc = documentService.createForSeries(seriesOpt.get());

            return doc.getId();

        } else {
            throw new ResponseStatusException(HttpStatusCode.valueOf(404), "No series");
        }

    }

    @PostMapping(value="documents")
    public UUID postDocument(@RequestBody DocumentDTO dto, @RequestParam(name = "token", required = false) String token) {

        //"security"
        accessService.adminOrThrow(token);


        DocumentModel doc = new DocumentModel();

        doc.setWeek(dto.getWeek());
        doc.setTemplate(templateRepository.getReferenceById(dto.getTemplateId()));

        documentRepository.save(doc);
        
        return doc.getId();
    }

    @PostMapping(value="documents/{docId}/change/{variable}")
    public String changeDocumentVariable(@PathVariable("docId") UUID docId, @RequestParam(name = "token", required = false) String token, @PathVariable("variable") String variable, @RequestBody String value) {

        //"security"
        accessService.adminOrThrow(token);

        var opt = documentRepository.findById(docId);

        if (opt.isPresent()) {
            var doc = opt.get();

            switch (variable) {
                case "public":
                    return Boolean.toString(setPublic(doc, value));
                case "editable":
                    return Boolean.toString(setEditable(doc, value));
                case "template":
                    return setTemplate(doc, value).toString();
                case "date":
                    return dateFormatter.format(setDate(doc, value));
            }

            documentRepository.save(opt.get());
        }

        return Boolean.toString(opt.get().isPublic());
    }

    static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("YYYY-MM-dd");

    private LocalDate setDate(DocumentModel doc, String value) {

        LocalDate date = LocalDate.parse(value, dateFormatter);

        doc.setWeek(date);
        documentRepository.save(doc);

        return doc.getWeek();

    }

    private UUID setTemplate(DocumentModel doc, String value) {
        UUID templateId = UUID.fromString(value);

        var opt = templateRepository.findById(templateId);

        if (!opt.isPresent()) throw new ResponseStatusException(HttpStatusCode.valueOf(404), "No template");

        doc.setTemplate(opt.get());
        documentRepository.save(doc);

        return doc.getTemplate().getId();
    }

    private boolean setEditable(DocumentModel doc, String value) {
        boolean isEditable = Boolean.parseBoolean(value);

        doc.setEditable(isEditable);
        documentRepository.save(doc);

        return doc.isEditable();
    }

    private boolean setPublic(DocumentModel doc, String value) {

        boolean isPublic = Boolean.parseBoolean(value);

        doc.setPublic(isPublic);
        documentRepository.save(doc);

        return doc.isPublic();
    }

    @GetMapping(value="documents/{uuid}/change/public/{public}")
    public String changeIsPublic(@PathVariable("uuid") UUID id, @RequestParam(name = "token", required = false) String token, @PathVariable("public") Boolean isPublic) {

        //"security"
        accessService.adminOrThrow(token);

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
        accessService.adminOrThrow(token);

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
        accessService.adminOrThrow(token);

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
        accessService.adminOrThrow(token);

        var opt = documentRepository.findById(id);

        if (opt.isPresent()) {
            var model = opt.get();
            model.setTemplate(templateRepository.getReferenceById(dto.getTemplateId()));
            model.setWeek(dto.getWeek());
            documentRepository.save(model);
            return "ok";
        } else {
            return "no document";
        }
    }
}
