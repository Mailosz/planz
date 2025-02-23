package pl.mo.planz.controllers;

import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import pl.mo.planz.repositories.PermissionRepository;
import pl.mo.planz.services.AccessService;
import pl.mo.planz.view.PageBuilder;
import pl.mo.planz.view.TemplatesManager;

@RestController
public class AdminPagesController {

    @Autowired
    AccessService accessService;

    @Autowired
    PermissionRepository permissionRepository;
    
    
    @GetMapping(value="/admin/config/permissions/{token}")
    public String permissionsList(@PathVariable(name = "token") String token) {
        
        // AccessObject access = accessService.getAccess(token, "admin");

        var permissions = permissionRepository.findAll();

        String content = permissions.stream()
            .map((p) -> {
                String permissionString = "<div><div class=\"permission-name\">" + p.getName() + "</div>";
                if (p.getDescription() != null) {
                    permissionString += "<div class=\"permission-description\">" + p.getDescription() + "</div>";
                }
                return permissionString + "</div>";
            })
            .collect(Collectors.joining());


        
        return PageBuilder.buildView(TemplatesManager.getTemplate("views/admin/access/permissions.view.html"), Map.of("content", () -> content));
    }

    
}
