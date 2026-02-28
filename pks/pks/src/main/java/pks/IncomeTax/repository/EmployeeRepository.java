package pks.IncomeTax.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pks.IncomeTax.model.Employee;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

	Optional<Employee> findByPan(String pan);

	Optional<Employee> findByPanIgnoreCase(String pan);

	boolean existsByPanIgnoreCase(String pan);

}
