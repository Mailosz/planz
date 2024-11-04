package pl.mo.planz.controllers;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import pl.mo.planz.dto.DatalistItemDTO;
import pl.mo.planz.model.DatalistModel;
import pl.mo.planz.model.DatalistValueModel;
import pl.mo.planz.repositories.DocumentRepository;
import pl.mo.planz.repositories.FieldRepository;
import pl.mo.planz.repositories.FieldValueHistoryRepository;
import pl.mo.planz.repositories.FieldValueRepository;
import pl.mo.planz.repositories.IdentityRepository;
import pl.mo.planz.repositories.PermissionRepository;
import pl.mo.planz.repositories.TemplateRepository;
import pl.mo.planz.repositories.DatalistRepository;
import pl.mo.planz.repositories.DatalistValueRepository;
import pl.mo.planz.services.AccessService;
import pl.mo.planz.services.IdentityService;

@RestController
@CrossOrigin
public class DatalistController {

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
    DatalistRepository listValuesRepository;

    @Autowired
    FieldRepository fieldRepository;

    @Autowired
    IdentityRepository identityRepository;

    @Autowired
    FieldValueHistoryRepository historyRepository;

    @Autowired
    IdentityService identityService;

    @Autowired
    AccessService accessService;

    @Autowired
    DatalistValueRepository datalistValueRepository;
    
    @GetMapping(value="datalist")
    public List<DatalistItemDTO> getAllDatalists(@RequestParam("token") String token) {

        accessService.adminOrThrow(token);

        return listRepository.findAll().stream().map((d) -> new DatalistItemDTO(d.getId(), d.getName(), d.getDescription())).collect(Collectors.toList());

    }

    @GetMapping(value="datalist/{id}")
    public List<String> getDatalistValues(@PathVariable("id") UUID dlId, @RequestParam("token") String token) {

        accessService.adminOrThrow(token);

        var listOpt = listRepository.findById(dlId);

        if (!listOpt.isPresent()) throw new ResponseStatusException(HttpStatusCode.valueOf(404), "No datalist");

        var list = listOpt.get();

        return list.getValues().stream().map((v) -> v.getValue()).collect(Collectors.toList());

    }

    @PutMapping(value="datalist/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    @Transactional
    public void setDatalistValues(@PathVariable("id") UUID dlId, @RequestBody(required = false) String body, @RequestParam(name = "name", required = false) String name, @RequestParam("token") String token) {

        accessService.adminOrThrow(token);

        var listOpt = listRepository.findById(dlId);

        DatalistModel list;
        if (!listOpt.isPresent()) {
            list = new DatalistModel();
            if (name == null) {
                throw new ResponseStatusException(HttpStatusCode.valueOf(404), "New datalist needs name");
            }
        } else {
            list = listOpt.get();
        }
        
        
        if (name != null) {
            list.setName(name);
            listRepository.save(list);
        }


        if (body != null) {
            datalistValueRepository.deleteAllValues(list);

            String[] values = body.split("\n");

            list.setValues(Arrays.stream(values)
            .filter((s) -> !s.isBlank())
            .map(v -> new DatalistValueModel(v, list))
            .collect(Collectors.toList()));

            listRepository.save(list);
        }

    }
}
