package pks.IncomeTax.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pks.IncomeTax.model.Employee;
import pks.IncomeTax.repository.EmployeeRepository;

import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/api/employee")
public class EmployeeController {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeController.class);

    private final EmployeeRepository repo;

    public EmployeeController(EmployeeRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    public ResponseEntity<Employee> create(@RequestBody Employee employee) {
        // Log incoming request to help diagnose duplicate submissions or payload issues
        logger.info("[{}] POST /api/employee received at {} on thread {}: name='{}', employeeId='{}', financialYear='{}', basicPay='{}'",
                Instant.now(), Instant.now().toString(), Thread.currentThread().getId(),
                employee.getName(), employee.getEmployeeId(), employee.getFinancialYear(), employee.getBasicPay());

        Employee saved = repo.save(employee);
        logger.info("[{}] Employee persisted with id={}", Instant.now(), saved.getId());
        return ResponseEntity.created(URI.create("/api/employee/" + saved.getId())).body(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> get(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
