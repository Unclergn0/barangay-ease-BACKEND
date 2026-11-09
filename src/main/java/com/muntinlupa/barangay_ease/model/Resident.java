
package com.muntinlupa.barangay_ease.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "residents")
public class Resident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resident_id")
    private Integer residentId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "house_street")
    private String houseStreet;

    @Column(name = "barangay")
    private String barangay;

    @Column(name = "barangay_id")
    private Integer barangayId;

    @Column(name = "email", nullable = false)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "security_pin", length = 255)
    private String securityPin;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Resident() {
    }

    public Integer getResidentId() {
        return residentId;
    }

    public Integer getId() {
        return residentId;
    }

    public void setResidentId(Integer residentId) {
        this.residentId = residentId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getHouseStreet() {
        return houseStreet;
    }

    public void setHouseStreet(String houseStreet) {
        this.houseStreet = houseStreet;
    }

    public String getBarangay() {
        return barangay;
    }

    public void setBarangay(String barangay) {
        this.barangay = barangay;
    }

    public Integer getBarangayId() {
        return barangayId;
    }

    public void setBarangayId(Integer barangayId) {
        this.barangayId = barangayId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSecurityPin() {
        return securityPin;
    }

    public void setSecurityPin(String securityPin) {
        this.securityPin = securityPin;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
