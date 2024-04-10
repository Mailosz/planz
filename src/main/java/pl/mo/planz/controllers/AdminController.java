package pl.mo.planz.controllers;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import pl.mo.planz.dto.AdminDTO;
import pl.mo.planz.model.SeriesModel;
import pl.mo.planz.repositories.SeriesRepository;
import pl.mo.planz.services.IdentityService;

@RestController
public class AdminController {
    
    @Autowired
    IdentityService identityService;

    @Autowired
    SeriesRepository seriesRepository;

    @GetMapping(value="/admin/{seriesId}")
    public AdminDTO createDocumentForSeries(@RequestParam(name = "token", required = false) String token, @PathVariable("seriesId") UUID seriesId) {
        identityService.adminOrThrow(token);
        
        Optional<SeriesModel> seriesOpt =  seriesRepository.findById(seriesId);

        if (!seriesOpt.isPresent()) throw new ResponseStatusException(HttpStatusCode.valueOf(404), "No series");


        AdminDTO dto = new AdminDTO();
        dto.setSeriesName(seriesOpt.get().getName());
        dto.setAccessList(new ArrayList<>());

        return dto;

    }

}
