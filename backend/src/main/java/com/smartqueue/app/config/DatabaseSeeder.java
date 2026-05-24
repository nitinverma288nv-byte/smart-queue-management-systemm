package com.smartqueue.app.config;

import com.smartqueue.app.entity.*;
import com.smartqueue.app.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired private UserRepository userRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private HospitalBranchRepository hospitalBranchRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private BankRepository bankRepository;
    @Autowired private BankBranchRepository bankBranchRepository;
    @Autowired private CollegeRepository collegeRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private StaffRepository staffRepository;
    @Autowired private CounterRepository counterRepository;
    @Autowired private SlotRepository slotRepository;
    @Autowired private QueueRepository queueRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TokenSequenceRepository tokenSequenceRepository;

    @Override
    public void run(String... args) throws Exception {

        // ─── TOKEN SEQUENCES ───────────────────────────────────────────────────
        if (!tokenSequenceRepository.existsById("HOSPITAL")) {
            tokenSequenceRepository.save(TokenSequence.builder().sectorType("HOSPITAL").lastValue(100).build());
        }
        if (!tokenSequenceRepository.existsById("BANK")) {
            tokenSequenceRepository.save(TokenSequence.builder().sectorType("BANK").lastValue(100).build());
        }
        if (!tokenSequenceRepository.existsById("COLLEGE")) {
            tokenSequenceRepository.save(TokenSequence.builder().sectorType("COLLEGE").lastValue(100).build());
        }

        // ─── GUARD: Already seeded? ────────────────────────────────────────────
        boolean alreadySeeded = userRepository.findByUsername("_pankaj_09").isPresent()
                || hospitalRepository.count() > 0
                || bankRepository.count() > 0
                || collegeRepository.count() > 0;

        if (alreadySeeded) {
            return;
        }

        // ══════════════════════════════════════════════════════════════════════
        // 1.  USERS & STAFF ACCOUNTS (Sabka password SAME: Root@123, Admin: Pankaj@123)
        // ══════════════════════════════════════════════════════════════════════

        // Admin Account
        saveUser("_pankaj_09", "Pankaj@123", "pankaj09@queue.com", "System Administrator", Role.ROLE_ADMIN, null);

        // Regular test user
        saveUser("user", "Root@123", "user@queue.com", "Test User", Role.ROLE_USER, null);

        // ── Hospital Staff ────────────────────────────────────────────────────
        User nishi   = saveUser("nishi",   "Root@123", "nishi@queue.com",   "Apollo Main Staff",   Role.ROLE_STAFF, "HOSPITAL");
        User nitin   = saveUser("nitin",   "Root@123", "nitin@queue.com",   "Apollo City Staff",   Role.ROLE_STAFF, "HOSPITAL");
        User nandani = saveUser("nandani", "Root@123", "nandani@queue.com", "Bombay Main Staff",   Role.ROLE_STAFF, "HOSPITAL");
        User nandu   = saveUser("nandu",   "Root@123", "nandu@queue.com",   "Bombay City Staff",   Role.ROLE_STAFF, "HOSPITAL");
        User nittu   = saveUser("nittu",   "Root@123", "nittu@queue.com",   "CHL Main Staff",      Role.ROLE_STAFF, "HOSPITAL");
        User nishu   = saveUser("nishu",   "Root@123", "nishu@queue.com",   "CHL City Staff",      Role.ROLE_STAFF, "HOSPITAL");
        User lucky   = saveUser("lucky",   "Root@123", "lucky@queue.com",   "Vishesh Main Staff",  Role.ROLE_STAFF, "HOSPITAL");
        User aarav   = saveUser("aarav",   "Root@123", "aarav@queue.com",   "Vishesh City Staff",  Role.ROLE_STAFF, "HOSPITAL");
        User vivaan  = saveUser("vivaan",  "Root@123", "vivaan@queue.com",  "GK Main Staff",       Role.ROLE_STAFF, "HOSPITAL");
        User reyansh = saveUser("reyansh", "Root@123", "reyansh@queue.com", "GK City Staff",       Role.ROLE_STAFF, "HOSPITAL");

        // ── Bank Staff ────────────────────────────────────────────────────────
        User oakes   = saveUser("oakes",   "Root@123", "oakes@queue.com",   "SBI Main Staff",      Role.ROLE_STAFF, "BANK");
        User dixon   = saveUser("dixon",   "Root@123", "dixon@queue.com",   "SBI City Staff",      Role.ROLE_STAFF, "BANK");
        User krish   = saveUser("krish",   "Root@123", "krish@queue.com",   "HDFC Main Staff",     Role.ROLE_STAFF, "BANK");
        User arjun   = saveUser("arjun",   "Root@123", "arjun@queue.com",   "HDFC City Staff",     Role.ROLE_STAFF, "BANK");
        User vihaan  = saveUser("vihaan",  "Root@123", "vihaan@queue.com",  "ICICI Main Staff",    Role.ROLE_STAFF, "BANK");
        User advik   = saveUser("advik",   "Root@123", "advik@queue.com",   "ICICI City Staff",    Role.ROLE_STAFF, "BANK");
        User kabir   = saveUser("kabir",   "Root@123", "kabir@queue.com",   "Axis Main Staff",     Role.ROLE_STAFF, "BANK");
        User viraj   = saveUser("viraj",   "Root@123", "viraj@queue.com",   "Axis City Staff",     Role.ROLE_STAFF, "BANK");
        User ansh    = saveUser("ansh",    "Root@123", "ansh@queue.com",    "BOB Main Staff",      Role.ROLE_STAFF, "BANK");
        User dev     = saveUser("dev",     "Root@123", "dev@queue.com",     "BOB City Staff",      Role.ROLE_STAFF, "BANK");

        // ── College Staff ─────────────────────────────────────────────────────
        User pari    = saveUser("pari",    "Root@123", "pari@queue.com",    "SGSITS Main Staff",   Role.ROLE_STAFF, "COLLEGE");
        User riya    = saveUser("riya",    "Root@123", "riya@queue.com",    "Medicaps Staff",      Role.ROLE_STAFF, "COLLEGE");
        User kavya   = saveUser("kavya",   "Root@123", "kavya@queue.com",   "IPS Staff",           Role.ROLE_STAFF, "COLLEGE");
        User tara    = saveUser("tara",    "Root@123", "tara@queue.com",    "Acropolis Staff",     Role.ROLE_STAFF, "COLLEGE");
        User meher   = saveUser("meher",   "Root@123", "meher@queue.com",   "IET DAVV Staff",      Role.ROLE_STAFF, "COLLEGE");
        User aisha   = saveUser("aisha",   "Root@123", "aisha@queue.com",   "CDGI Staff",          Role.ROLE_STAFF, "COLLEGE");

        // ══════════════════════════════════════════════════════════════════════
        // 2.  HOSPITAL SECTOR — 5 Hospitals × 2 Branches × 5 Unique Doctors
        // ══════════════════════════════════════════════════════════════════════

        // ── APOLLO HOSPITAL ───────────────────────────────────────────────────
        Hospital apollo = saveHospital("Apollo Hospital");

        HospitalBranch apolloMain = saveBranch(apollo, "Apollo Hospital Main Branch", "Vijay Nagar, Indore", 22.7196, 75.8577);
        addDoctor(apolloMain, "Dr. Rajesh Sharma",  "Cardiologist",    1000, true);
        addDoctor(apolloMain, "Dr. Priya Mehta",    "Neurologist",     1200, false);
        addDoctor(apolloMain, "Dr. Rohit Jain",     "Orthopedic",       900, true);
        addDoctor(apolloMain, "Dr. Neha Joshi",     "ENT Specialist",   700, false);
        addDoctor(apolloMain, "Dr. Amit Verma",     "Dermatologist",    800, true);
        seedCounter("OPD Counter", "HOSPITAL", apolloMain.getId(), nishi);

        HospitalBranch apolloCity = saveBranch(apollo, "Apollo Hospital City Branch", "Palasia, Indore", 22.7210, 75.8560);
        addDoctor(apolloCity, "Dr. Akash Verma",    "ENT Specialist",   700, true);
        addDoctor(apolloCity, "Dr. Sneha Kapoor",   "Dermatologist",    850, false);
        addDoctor(apolloCity, "Dr. Karan Tiwari",   "Cardiologist",    1100, true);
        addDoctor(apolloCity, "Dr. Pooja Arora",    "Gynecologist",     950, true);
        addDoctor(apolloCity, "Dr. Mohit Soni",     "Pediatrician",     750, false);
        seedCounter("OPD Counter", "HOSPITAL", apolloCity.getId(), nitin);

        // ── BOMBAY HOSPITAL ───────────────────────────────────────────────────
        Hospital bombay = saveHospital("Bombay Hospital");

        HospitalBranch bombayMain = saveBranch(bombay, "Bombay Hospital Main Branch", "MG Road, Indore", 22.7180, 75.8610);
        addDoctor(bombayMain, "Dr. Vikram Singh",   "Cardiologist",    1200, true);
        addDoctor(bombayMain, "Dr. Pooja Khanna",   "Neurologist",     1300, false);
        addDoctor(bombayMain, "Dr. Ashish Patel",   "Orthopedic",       900, true);
        addDoctor(bombayMain, "Dr. Sneha Jain",     "Pediatrician",     750, true);
        addDoctor(bombayMain, "Dr. Kunal Shah",     "ENT Specialist",   650, false);
        seedCounter("OPD Counter", "HOSPITAL", bombayMain.getId(), nandani);

        HospitalBranch bombayCity = saveBranch(bombay, "Bombay Hospital City Branch", "Scheme 54, Indore", 22.7165, 75.8625);
        addDoctor(bombayCity, "Dr. Ritesh Sharma",  "Dermatologist",    850, true);
        addDoctor(bombayCity, "Dr. Niharika Rao",   "Gynecologist",     950, false);
        addDoctor(bombayCity, "Dr. Aditya Gupta",   "Orthopedic",       900, true);
        addDoctor(bombayCity, "Dr. Vivek Jain",     "Cardiologist",    1150, true);
        addDoctor(bombayCity, "Dr. Tanya Mehra",    "Neurologist",     1250, false);
        seedCounter("OPD Counter", "HOSPITAL", bombayCity.getId(), nandu);

        // ── CHL HOSPITAL ──────────────────────────────────────────────────────
        Hospital chl = saveHospital("CHL Hospital");

        HospitalBranch chlMain = saveBranch(chl, "CHL Hospital Main Branch", "A.B. Road, Indore", 22.7225, 75.8540);
        addDoctor(chlMain, "Dr. Aditya Mehra",  "General Physician",  500, true);
        addDoctor(chlMain, "Dr. Simran Kaur",   "Gynecologist",       950, false);
        addDoctor(chlMain, "Dr. Rahul Soni",    "Orthopedic",         850, true);
        addDoctor(chlMain, "Dr. Neeraj Gupta",  "Neurologist",       1200, true);
        addDoctor(chlMain, "Dr. Kavita Rao",    "Dermatologist",      800, false);
        seedCounter("OPD Counter", "HOSPITAL", chlMain.getId(), nittu);

        HospitalBranch chlCity = saveBranch(chl, "CHL Hospital City Branch", "Rajwada, Indore", 22.7200, 75.8556);
        addDoctor(chlCity, "Dr. Preet Malhotra", "Cardiologist",     1050, true);
        addDoctor(chlCity, "Dr. Anita Gupta",    "Pediatrician",      700, false);
        addDoctor(chlCity, "Dr. Suresh Tiwari",  "ENT Specialist",    650, true);
        addDoctor(chlCity, "Dr. Renu Sharma",    "Dermatologist",     800, true);
        addDoctor(chlCity, "Dr. Aman Joshi",     "General Physician", 450, false);
        seedCounter("OPD Counter", "HOSPITAL", chlCity.getId(), nishu);

        // ── VISHESH JUPITER HOSPITAL ──────────────────────────────────────────
        Hospital vishesh = saveHospital("Vishesh Jupiter Hospital");

        HospitalBranch visheshMain = saveBranch(vishesh, "Vishesh Jupiter Main Branch", "Airport Road, Indore", 22.7260, 75.8500);
        addDoctor(visheshMain, "Dr. Akash Tiwari",  "Cardiologist",    1000, true);
        addDoctor(visheshMain, "Dr. Priti Agrawal", "ENT Specialist",   700, false);
        addDoctor(visheshMain, "Dr. Manish Dubey",  "Orthopedic",       850, true);
        addDoctor(visheshMain, "Dr. Harsh Vyas",    "Neurologist",     1300, true);
        addDoctor(visheshMain, "Dr. Tanya Kapoor",  "Pediatrician",     750, false);
        seedCounter("OPD Counter", "HOSPITAL", visheshMain.getId(), lucky);

        HospitalBranch visheshCity = saveBranch(vishesh, "Vishesh Jupiter City Branch", "Sapna Sangeeta Road, Indore", 22.7240, 75.8518);
        addDoctor(visheshCity, "Dr. Leena Sharma",   "Gynecologist",   950, true);
        addDoctor(visheshCity, "Dr. Ramesh Patil",   "Cardiologist",  1100, false);
        addDoctor(visheshCity, "Dr. Neetu Jain",     "Dermatologist",  850, true);
        addDoctor(visheshCity, "Dr. Sunil Rawat",    "Orthopedic",     900, true);
        addDoctor(visheshCity, "Dr. Priya Bansal",   "Neurologist",   1200, false);
        seedCounter("OPD Counter", "HOSPITAL", visheshCity.getId(), aarav);

        // ── GREATER KAILASH HOSPITAL ──────────────────────────────────────────
        Hospital gk = saveHospital("Greater Kailash Hospital");

        HospitalBranch gkMain = saveBranch(gk, "Greater Kailash Main Branch", "Mahalaxmi Nagar, Indore", 22.7158, 75.8648);
        addDoctor(gkMain, "Dr. Deepak Sharma",   "Cardiologist",    1100, true);
        addDoctor(gkMain, "Dr. Isha Verma",      "Dermatologist",    850, false);
        addDoctor(gkMain, "Dr. Mohit Jain",      "Orthopedic",       900, true);
        addDoctor(gkMain, "Dr. Ruchi Gupta",     "Gynecologist",     950, true);
        addDoctor(gkMain, "Dr. Abhishek Singh",  "ENT Specialist",   700, false);
        seedCounter("OPD Counter", "HOSPITAL", gkMain.getId(), vivaan);

        HospitalBranch gkCity = saveBranch(gk, "Greater Kailash City Branch", "Bhawarkuan, Indore", 22.7142, 75.8665);
        addDoctor(gkCity, "Dr. Sandeep Chauhan",  "Neurologist",    1150, true);
        addDoctor(gkCity, "Dr. Alka Trivedi",     "Pediatrician",    700, false);
        addDoctor(gkCity, "Dr. Rajan Mishra",     "General Physician", 500, true);
        addDoctor(gkCity, "Dr. Sonal Jain",       "Gynecologist",    950, true);
        addDoctor(gkCity, "Dr. Nikhil Verma",     "Cardiologist",   1100, false);
        seedCounter("OPD Counter", "HOSPITAL", gkCity.getId(), reyansh);

        // ══════════════════════════════════════════════════════════════════════
        // 3.  BANK SECTOR — 5 Banks × 2 Branches
        // ══════════════════════════════════════════════════════════════════════
        String[] bankNames = {"SBI", "HDFC", "ICICI", "Axis Bank", "Bank of Baroda"};

        for (int bi = 0; bi < bankNames.length; bi++) {
            Bank bank = bankRepository.save(Bank.builder()
                    .name(bankNames[bi])
                    .logoUrl("https://images.unsplash.com/photo-1541354451442-952fe93efa3c?w=100")
                    .build());

            for (int br = 1; br <= 2; br++) {
                BankBranch branch = bankBranchRepository.save(BankBranch.builder()
                        .bank(bank)
                        .name(bankNames[bi] + (br == 1 ? " Vijay Nagar Branch" : " Palasia Branch"))
                        .location("Indore, MP")
                        .latitude(22.7200 + bi * 0.001 + br * 0.0005)
                        .longitude(75.8600 + bi * 0.001 - br * 0.0005)
                        .build());

                User mainBankStaff;
                if (bi == 0) mainBankStaff = (br == 1) ? oakes : dixon;
                else if (bi == 1) mainBankStaff = (br == 1) ? krish : arjun;
                else if (bi == 2) mainBankStaff = (br == 1) ? vihaan : advik;
                else if (bi == 3) mainBankStaff = (br == 1) ? kabir : viraj;
                else mainBankStaff = (br == 1) ? ansh : dev;

                seedCounter("Cash Counter", "BANK", branch.getId(), mainBankStaff);
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // 4.  COLLEGE SECTOR — 6 Colleges × 3 Departments (di=1 Main Campus, di=2 City Campus, di=3 Engineering Block)
        // ══════════════════════════════════════════════════════════════════════
        String[] collegeNames = {"SGSITS", "Medicaps", "IPS Academy", "Acropolis", "IET DAVV", "Chameli Devi Group of Institutions"};

        for (int bi = 0; bi < collegeNames.length; bi++) {
            College college = collegeRepository.save(College.builder()
                    .name(collegeNames[bi])
                    .logoUrl("https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=100")
                    .build());

            for (int di = 1; di <= 3; di++) {
                String deptName = "";
                String officeName = "";
                if (di == 1) {
                    deptName = "Main Campus";
                    officeName = "Admin Block 1";
                } else if (di == 2) {
                    deptName = "City Campus";
                    officeName = "Admin Block 2";
                } else {
                    deptName = "Engineering Block";
                    officeName = "Engineering Admin";
                }

                Department dept = departmentRepository.save(Department.builder()
                        .college(college)
                        .name(deptName)
                        .buildingDetails("Indore, MP")
                        .officeName(officeName)
                        .build());

                User mainCollegeStaff = null;
                if (di == 1 || di == 3) {
                    if (bi == 0) mainCollegeStaff = pari;
                    else if (bi == 1) mainCollegeStaff = riya;
                    else if (bi == 2) mainCollegeStaff = kavya;
                    else if (bi == 3) mainCollegeStaff = tara;
                    else if (bi == 4) mainCollegeStaff = meher;
                    else mainCollegeStaff = aisha;

                    seedCollegeCounter("Student Help Desk", "COLLEGE", dept.getId(), mainCollegeStaff);
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ══════════════════════════════════════════════════════════════════════════

    private User saveUser(String username, String password, String email, String fullName, Role role, String staffType) {
        return userRepository.findByUsername(username).orElseGet(() ->
                userRepository.findByEmail(email).orElseGet(() -> {
                    User u = User.builder()
                            .username(username)
                            .password(passwordEncoder.encode(password))
                            .email(email)
                            .fullName(fullName)
                            .role(role)
                            .staffType(staffType)
                            .build();
                    return userRepository.save(u);
                })
        );
    }

    private Hospital saveHospital(String name) {
        return hospitalRepository.save(Hospital.builder()
                .name(name + " Indore")
                .logoUrl("https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?w=100")
                .build());
    }

    private HospitalBranch saveBranch(Hospital hospital, String name, String location, double lat, double lon) {
        return hospitalBranchRepository.save(HospitalBranch.builder()
                .hospital(hospital)
                .name(name)
                .location(location)
                .latitude(lat)
                .longitude(lon)
                .build());
    }

    private void addDoctor(HospitalBranch branch, String name, String specialization, int fee, boolean available) {
        Doctor doctor = doctorRepository.save(Doctor.builder()
                .branch(branch)
                .hospitalId(branch.getHospital().getId())
                .name(name)
                .specialization(specialization)
                .specialty(specialization)
                .consultationFee(fee)
                .availabilityStatus(available)
                .build());
        // Seed slots for this doctor for the next 15 days
        seedDoctorSlots(branch.getId(), doctor.getId());
    }

    private void seedCounter(String counterName, String sectorType, Long branchId, User staffUser) {
        Counter counter = counterRepository.save(Counter.builder()
                .counterName(counterName)
                .counterType(counterName)
                .sectorType(sectorType)
                .branchId(branchId)
                .status("ACTIVE")
                .build());

        Staff staff = staffRepository.save(Staff.builder()
                .user(staffUser)
                .sectorType(sectorType)
                .referenceId(branchId)
                .counter(counter)
                .build());

        counter.setAssignedStaffId(staff.getId());
        counterRepository.save(counter);

        queueRepository.save(Queue.builder()
                .sectorType(sectorType)
                .referenceId(branchId)
                .branchId(branchId)
                .counterId(counter.getId())
                .assignedStaffId(staff.getId())
                .lastTokenNumber(100)
                .build());

        // Seed 15 days of slots for bank counters
        for (int day = 0; day < 15; day++) {
            LocalDate date = LocalDate.now().plusDays(day);
            seedSlot(sectorType, counter.getId(), branchId, counter.getId(), date);
        }
    }

    private void seedCollegeCounter(String counterName, String sectorType, Long deptId, User staffUser) {
        Counter counter = counterRepository.save(Counter.builder()
                .counterName(counterName)
                .counterType(counterName)
                .sectorType(sectorType)
                .branchId(deptId)
                .status("ACTIVE")
                .build());

        Staff staff = staffRepository.save(Staff.builder()
                .user(staffUser)
                .sectorType(sectorType)
                .referenceId(deptId)
                .counter(counter)
                .build());

        counter.setAssignedStaffId(staff.getId());
        counterRepository.save(counter);

        queueRepository.save(Queue.builder()
                .sectorType(sectorType)
                .referenceId(deptId)
                .branchId(deptId)
                .counterId(counter.getId())
                .assignedStaffId(staff.getId())
                .lastTokenNumber(100)
                .build());

        for (int day = 0; day < 15; day++) {
            LocalDate date = LocalDate.now().plusDays(day);
            seedSlot(sectorType, counter.getId(), deptId, counter.getId(), date);
        }
    }

    private void seedSlot(String type, Long referenceId, Long branchId, Long counterId, LocalDate date) {
        slotRepository.save(Slot.builder().type(type).referenceId(referenceId).sectorType(type).branchId(branchId).counterId(counterId).date(date).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(9, 30)).maxTokens(10).bookedTokens(0).availability(true).maxCapacity(10).build());
        slotRepository.save(Slot.builder().type(type).referenceId(referenceId).sectorType(type).branchId(branchId).counterId(counterId).date(date).startTime(LocalTime.of(9, 30)).endTime(LocalTime.of(10, 0)).maxTokens(10).bookedTokens(0).availability(true).maxCapacity(10).build());
        slotRepository.save(Slot.builder().type(type).referenceId(referenceId).sectorType(type).branchId(branchId).counterId(counterId).date(date).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(10, 30)).maxTokens(10).bookedTokens(0).availability(true).maxCapacity(10).build());
        slotRepository.save(Slot.builder().type(type).referenceId(referenceId).sectorType(type).branchId(branchId).counterId(counterId).date(date).startTime(LocalTime.of(10, 30)).endTime(LocalTime.of(11, 0)).maxTokens(10).bookedTokens(0).availability(true).maxCapacity(10).build());
        slotRepository.save(Slot.builder().type(type).referenceId(referenceId).sectorType(type).branchId(branchId).counterId(counterId).date(date).startTime(LocalTime.of(11, 0)).endTime(LocalTime.of(11, 30)).maxTokens(10).bookedTokens(0).availability(true).maxCapacity(10).build());
    }

    private void seedDoctorSlots(Long branchId, Long doctorId) {
        for (int day = 0; day < 15; day++) {
            LocalDate date = LocalDate.now().plusDays(day);
            seedSlot("DOCTOR", doctorId, branchId, null, date);
        }
    }
}
