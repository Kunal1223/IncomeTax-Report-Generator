package pks.IncomeTax.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pks.IncomeTax.model.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

}
