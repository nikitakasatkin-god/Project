package org.example.config;

import org.example.model.*;
import org.example.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DivisionRepository divisionRepository;
    private final CarrierRepository carrierRepository;
    private final VehicleRepository vehicleRepository;
    private final PlantRepository plantRepository;
    private final WarehouseRepository warehouseRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           DivisionRepository divisionRepository,
                           CarrierRepository carrierRepository,
                           VehicleRepository vehicleRepository,
                           PlantRepository plantRepository,
                           WarehouseRepository warehouseRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.divisionRepository = divisionRepository;
        this.carrierRepository = carrierRepository;
        this.vehicleRepository = vehicleRepository;
        this.plantRepository = plantRepository;
        this.warehouseRepository = warehouseRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Create divisions (только если их еще нет)
        Division div1 = divisionRepository.findByName("Логистический центр №1").orElse(null);
        if (div1 == null) {
            div1 = new Division();
            div1.setName("Логистический центр №1");
            divisionRepository.save(div1);
        }

        Division div2 = divisionRepository.findByName("Логистический центр №2").orElse(null);
        if (div2 == null) {
            div2 = new Division();
            div2.setName("Логистический центр №2");
            divisionRepository.save(div2);
        }

        // Create admin (только если не существует)
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("Администратор Системы");
            admin.setRole(Role.ADMIN);
            admin.setDivision(div1);
            admin.setActive(true);
            userRepository.save(admin);
        }

        // Create logist
        if (userRepository.findByUsername("logist").isEmpty()) {
            User logist = new User();
            logist.setUsername("logist");
            logist.setPassword(passwordEncoder.encode("logist123"));
            logist.setFullName("Логист Иванов И.И.");
            logist.setRole(Role.LOGIST);
            logist.setDivision(div1);
            logist.setActive(true);
            userRepository.save(logist);
        }

        // Create dispatcher
        if (userRepository.findByUsername("dispatcher").isEmpty()) {
            User dispatcher = new User();
            dispatcher.setUsername("dispatcher");
            dispatcher.setPassword(passwordEncoder.encode("dispatcher123"));
            dispatcher.setFullName("Диспетчер Петров П.П.");
            dispatcher.setRole(Role.DISPATCHER);
            dispatcher.setDivision(div1);
            dispatcher.setActive(true);
            userRepository.save(dispatcher);
        }

        // Create carriers (только если их нет)
        if (carrierRepository.count() == 0) {
            Carrier carrier1 = new Carrier();
            carrier1.setName("ТрансЛогистик");
            carrier1.setContactPerson("Сидоров А.А.");
            carrier1.setPhone("+7(999)123-45-67");
            carrierRepository.save(carrier1);

            Carrier carrier2 = new Carrier();
            carrier2.setName("ГрузоСервис");
            carrier2.setContactPerson("Кузнецов В.В.");
            carrier2.setPhone("+7(999)234-56-78");
            carrierRepository.save(carrier2);

            // Create vehicles
            Vehicle v1 = new Vehicle();
            v1.setPlateNumber("А123ВВ77");
            v1.setBrand("KAMAZ");
            v1.setModel("5490");
            v1.setDriverName("Водитель1");
            v1.setCarrier(carrier1);
            vehicleRepository.save(v1);

            Vehicle v2 = new Vehicle();
            v2.setPlateNumber("В456СС77");
            v2.setBrand("MAN");
            v2.setModel("TGX");
            v2.setDriverName("Водитель2");
            v2.setCarrier(carrier2);
            vehicleRepository.save(v2);
        }

        // Create plants (только если их нет)
        if (plantRepository.count() == 0) {
            Plant plant1 = new Plant();
            plant1.setName("Завод №1");
            plant1.setAddress("г. Москва, ул. Заводская, 1");
            plantRepository.save(plant1);

            Plant plant2 = new Plant();
            plant2.setName("Завод №2");
            plant2.setAddress("г. Санкт-Петербург, ул. Промышленная, 15");
            plantRepository.save(plant2);
        }

        // Create warehouses (только если их нет)
        if (warehouseRepository.count() == 0) {
            Warehouse wh1 = new Warehouse();
            wh1.setName("Склад №1");
            wh1.setAddress("г. Москва, ул. Складская, 5");
            warehouseRepository.save(wh1);

            Warehouse wh2 = new Warehouse();
            wh2.setName("Склад №2");
            wh2.setAddress("г. Санкт-Петербург, ул. Логистическая, 10");
            warehouseRepository.save(wh2);
        }

        System.out.println("=== Data initialization completed ===");
        System.out.println("Users: " + userRepository.count());
        System.out.println("Divisions: " + divisionRepository.count());
        System.out.println("Carriers: " + carrierRepository.count());
        System.out.println("Plants: " + plantRepository.count());
        System.out.println("Warehouses: " + warehouseRepository.count());
    }
}