package pks.IncomeTax.model;

import jakarta.persistence.*;

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
    private String employerTan;
    private String treasuryName;

    private Long basicPay;
    private Long da;
    private Long ta;
    private Long hra;
    private Long medicalAllowances;

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

    public String getEmployerTan() { return employerTan; }
    public void setEmployerTan(String employerTan) { this.employerTan = employerTan; }

    public String getTreasuryName() { return treasuryName; }
    public void setTreasuryName(String treasuryName) { this.treasuryName = treasuryName; }

    // Compatibility: some clients may send/expect the misspelling "tragary".
    public String getTragary() { return treasuryName; }
    public void setTragary(String tragary) { this.treasuryName = tragary; }

    public Long getBasicPay() { return basicPay; }
    public void setBasicPay(Long basicPay) { this.basicPay = basicPay; }

    public Long getDa() { return da; }
    public void setDa(Long da) { this.da = da; }

    public Long getTa() { return ta; }
    public void setTa(Long ta) { this.ta = ta; }

    public Long getHra() { return hra; }
    public void setHra(Long hra) { this.hra = hra; }

    public Long getMedicalAllowances() { return medicalAllowances; }
    public void setMedicalAllowances(Long medicalAllowances) { this.medicalAllowances = medicalAllowances; }

    public String getFinancialYear() { return financialYear; }
    public void setFinancialYear(String financialYear) { this.financialYear = financialYear; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
