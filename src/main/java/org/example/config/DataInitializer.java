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
    private final TripStatusRepository tripStatusRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           DivisionRepository divisionRepository,
                           CarrierRepository carrierRepository,
                           VehicleRepository vehicleRepository,
                           PlantRepository plantRepository,
                           WarehouseRepository warehouseRepository,
                           TripStatusRepository tripStatusRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.divisionRepository = divisionRepository;
        this.carrierRepository = carrierRepository;
        this.vehicleRepository = vehicleRepository;
        this.plantRepository = plantRepository;
        this.warehouseRepository = warehouseRepository;
        this.tripStatusRepository = tripStatusRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Create divisions
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

        // Create users
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

        // Create carriers
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

        // Create plants
        if (plantRepository.count() == 0) {
            String[][] plants = {
                    {"Завод №1", "г. Москва, ул. Заводская, 1"},
                    {"Завод №2", "г. Санкт-Петербург, ул. Промышленная, 15"},
                    {"Завод №3", "г. Нижний Новгород, ул. Индустриальная, 8"},
                    {"Завод №4", "г. Казань, ул. Производственная, 22"},
                    {"Завод №5", "г. Екатеринбург, ул. Станкостроительная, 5"}
            };
            for (String[] plant : plants) {
                Plant p = new Plant();
                p.setName(plant[0]);
                p.setAddress(plant[1]);
                plantRepository.save(p);
            }
        }

        // Create warehouses
        if (warehouseRepository.count() == 0) {
            String[][] warehouses = {
                    {"Склад №1", "г. Москва, ул. Складская, 5"},
                    {"Склад №2", "г. Санкт-Петербург, ул. Логистическая, 10"},
                    {"Склад №3", "г. Нижний Новгород, ул. Транспортная, 12"},
                    {"Склад №4", "г. Казань, ул. Приемная, 7"},
                    {"Склад №5", "г. Екатеринбург, ул. Отгрузочная, 3"}
            };
            for (String[] warehouse : warehouses) {
                Warehouse w = new Warehouse();
                w.setName(warehouse[0]);
                w.setAddress(warehouse[1]);
                warehouseRepository.save(w);
            }
        }

        // Initialize trip statuses
        initTripStatuses();

        System.out.println("=== Data initialization completed ===");
        System.out.println("Users: " + userRepository.count());
        System.out.println("Divisions: " + divisionRepository.count());
        System.out.println("Carriers: " + carrierRepository.count());
        System.out.println("Plants: " + plantRepository.count());
        System.out.println("Warehouses: " + warehouseRepository.count());
        System.out.println("Trip Statuses: " + tripStatusRepository.count());
    }

    private void initTripStatuses() {
        if (tripStatusRepository.count() == 0) {
            String[][] statuses = {
                    {"NEW", "Новый", "Новый рейс", "#6b7280", "1", "true"},
                    {"ARRIVED_LOADING", "Прибыл на погрузку", "ТС прибыло на погрузку", "#fef3c7", "2", "true"},
                    {"LOADED", "Погружен", "Груз погружен", "#dbeafe", "3", "true"},
                    {"IN_TRANSIT", "В пути", "Рейс в пути", "#eab308", "4", "true"},
                    {"ARRIVED_UNLOADING", "Прибыл на выгрузку", "ТС прибыло на выгрузку", "#fef3c7", "5", "true"},
                    {"UNLOADED", "Выгружен", "Груз выгружен", "#d1fae5", "6", "true"},
                    {"PROCESSED", "Обработан", "Рейс обработан", "#059669", "7", "true"},
                    {"CANCELLED", "Отменен", "Рейс отменен", "#ef4444", "8", "true"},
                    {"DELETED", "Удален", "Рейс удален", "#9ca3af", "9", "true"}
            };

            for (String[] status : statuses) {
                TripStatusEntity entity = new TripStatusEntity();
                entity.setCode(status[0]);
                entity.setName(status[1]);
                entity.setDescription(status[2]);
                entity.setColor(status[3]);
                entity.setSortOrder(Integer.parseInt(status[4]));
                entity.setSystemDefault(Boolean.parseBoolean(status[5]));
                entity.setActive(true);
                tripStatusRepository.save(entity);
            }
            System.out.println("Создано " + statuses.length + " системных статусов рейсов");
        }
    }
}