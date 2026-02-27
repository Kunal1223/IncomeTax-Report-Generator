package pks.IncomeTax.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String post;
    private String department;
    private String pan;
    private String employeeId;

    private BigDecimal basicPay;
    private BigDecimal da;
    private BigDecimal ta;
    private BigDecimal hra;
    private BigDecimal medicalAllowances;

    private String financialYear;

    private Instant createdAt = Instant.now();

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPost() { return post; }
    public void setPost(String post) { this.post = post; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getPan() { return pan; }
    public void setPan(String pan) { this.pan = pan; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public BigDecimal getBasicPay() { return basicPay; }
    public void setBasicPay(BigDecimal basicPay) { this.basicPay = basicPay; }

    public BigDecimal getDa() { return da; }
    public void setDa(BigDecimal da) { this.da = da; }

    public BigDecimal getTa() { return ta; }
    public void setTa(BigDecimal ta) { this.ta = ta; }

    public BigDecimal getHra() { return hra; }
    public void setHra(BigDecimal hra) { this.hra = hra; }

    public BigDecimal getMedicalAllowances() { return medicalAllowances; }
    public void setMedicalAllowances(BigDecimal medicalAllowances) { this.medicalAllowances = medicalAllowances; }

    public String getFinancialYear() { return financialYear; }
    public void setFinancialYear(String financialYear) { this.financialYear = financialYear; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
