package org.example.controller;

import org.example.model.*;
import org.example.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ApplicationController {

    private final RequestRepository requestRepository;
    private final DivisionRepository divisionRepository;
    private final CarrierRepository carrierRepository;
    private final PlantRepository plantRepository;
    private final WarehouseRepository warehouseRepository;
    private final TariffBrandedRepository tariffBrandedRepository;
    private final TariffNonBrandedRepository tariffNonBrandedRepository;

    public ApplicationController(RequestRepository requestRepository,
                                 DivisionRepository divisionRepository,
                                 CarrierRepository carrierRepository,
                                 PlantRepository plantRepository,
                                 WarehouseRepository warehouseRepository,
                                 TariffBrandedRepository tariffBrandedRepository,
                                 TariffNonBrandedRepository tariffNonBrandedRepository) {
        this.requestRepository = requestRepository;
        this.divisionRepository = divisionRepository;
        this.carrierRepository = carrierRepository;
        this.plantRepository = plantRepository;
        this.warehouseRepository = warehouseRepository;
        this.tariffBrandedRepository = tariffBrandedRepository;
        this.tariffNonBrandedRepository = tariffNonBrandedRepository;
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        return "profile";
    }

    @GetMapping("/references")
    public String references(Model model) {
        model.addAttribute("divisions", divisionRepository.findAll());
        model.addAttribute("carriers", carrierRepository.findAll());
        model.addAttribute("plants", plantRepository.findAll());
        model.addAttribute("warehouses", warehouseRepository.findAll());
        model.addAttribute("tariffsBranded", tariffBrandedRepository.findAll());
        model.addAttribute("tariffsNonBranded", tariffNonBrandedRepository.findAll());
        return "references";
    }

    @GetMapping("/settings")
    public String settings() {
        return "settings";
    }
}