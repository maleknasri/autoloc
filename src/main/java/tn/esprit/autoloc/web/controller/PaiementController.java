package tn.esprit.autoloc.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.autoloc.domain.Paiement;
import tn.esprit.autoloc.service.IPaiementService;

import java.util.List;

@RestController
@RequestMapping("/api/paiements")
@RequiredArgsConstructor
public class PaiementController {

    private final IPaiementService paiementService;

    @GetMapping("/{id}")
    public ResponseEntity<Paiement> findById(@PathVariable Long id) {
        Paiement paiement = paiementService.findById(id);
        return ResponseEntity.ok(paiement);
    }

    @GetMapping
    public ResponseEntity<List<Paiement>> findAll() {
        List<Paiement> paiements = paiementService.findAll();
        return ResponseEntity.ok(paiements);
    }
}
