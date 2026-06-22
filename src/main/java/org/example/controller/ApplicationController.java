package org.example.controller;

import org.example.model.*;
import org.example.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/requests")
    public String requests(@RequestParam(required = false) String type, Model model) {
        if (type != null) {
            model.addAttribute("currentType", type);
        }
        return "requests";
    }

    @GetMapping("/trips")
    public String trips(@RequestParam(required = false) String type, Model model) {
        if (type != null) {
            model.addAttribute("currentType", type);
        }
        return "trips";
    }

    @GetMapping("/request-detail")
    public String requestDetail(@RequestParam(required = false) Long id, Model model) {
        if (id != null) {
            model.addAttribute("requestId", id);
        }
        return "request-detail";
    }

    @GetMapping("/trip-detail")
    public String tripDetail(@RequestParam(required = false) Long id, Model model) {
        if (id != null) {
            model.addAttribute("tripId", id);
        }
        return "trip-detail";
    }

    @GetMapping("/trip-form")
    public String tripForm(@RequestParam(required = false) Long id, Model model) {
        if (id != null) {
            model.addAttribute("tripId", id);
        }
        return "trip-form";
    }

    @GetMapping("/request-form")
    public String requestForm(@RequestParam(required = false) Long id, Model model) {
        if (id != null) {
            model.addAttribute("requestId", id);
        }
        return "request-form";
    }

    @GetMapping("/profile")
    public String profile() {
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

    @GetMapping("/carrier-detail")
    public String carrierDetail(@RequestParam Long id, Model model) {
        model.addAttribute("carrierId", id);
        return "carrier-detail";
    }
}