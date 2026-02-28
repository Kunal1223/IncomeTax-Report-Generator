package pks.IncomeTax.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pks.IncomeTax.model.Employee;
import pks.IncomeTax.repository.EmployeeRepository;
import pks.IncomeTax.service.IncomeTaxReportService;

import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/employee")
public class EmployeeController {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeController.class);

    private final EmployeeRepository repo;
    private final IncomeTaxReportService reportService;

    public EmployeeController(EmployeeRepository repo, IncomeTaxReportService reportService) {
        this.repo = repo;
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<Employee> create(@RequestBody Employee employee) {
        // Log incoming request to help diagnose duplicate submissions or payload issues
        logger.info("[{}] POST /api/employee received at {} on thread {}: name='{}', employerTan='{}', treasury='{}', financialYear='{}', basicPay='{}',",
            Instant.now(), Instant.now().toString(), Thread.currentThread().getId(),
            employee.getName(), employee.getEmployerTan(), employee.getTreasuryName(), employee.getFinancialYear(), employee.getBasicPay());

        Employee saved = repo.save(employee);
        logger.info("[{}] Employee persisted with id={}", Instant.now(), saved.getId());
        return ResponseEntity.created(URI.create("/api/employee/" + saved.getId())).body(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> get(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<Map<String, String>> generateReport(@PathVariable Long id) {
        return repo.findById(id).map(emp -> {
            try {
                String filename = reportService.generate(emp);
                String downloadUrl = "/api/employee/report/download/" + filename;
                return ResponseEntity.ok(Map.of("downloadUrl", downloadUrl));
            } catch (Exception e) {
                logger.error("Failed to generate report", e);
                return ResponseEntity.status(500).body(Map.of("error", "Failed to generate report"));
            }
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/report/download/{filename:.+}")
    public ResponseEntity<Resource> downloadReport(@PathVariable String filename) {
        try {
            Path file = Path.of("reports").resolve(filename).toAbsolutePath();
            UrlResource resource = new UrlResource(file.toUri());
            if (!resource.exists()) return ResponseEntity.notFound().build();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error("Failed to serve report", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
