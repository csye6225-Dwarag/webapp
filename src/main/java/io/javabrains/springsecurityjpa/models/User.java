package io.javabrains.springsecurityjpa.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "email_id", nullable = false, unique = true)
    private String userName;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String firstName;
    private String lastName;
    private Timestamp accountCreated;
    private Timestamp accountUpdated;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private boolean active = true;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String roles;
    private boolean verified;
    private Timestamp verifiedOn;

    public User(String userName, String password, String firstName, String lastName){
        this.id = UUID.randomUUID();
        this.userName = userName;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.verified = false;
        this.verifiedOn = null;
    }

    public User(String userName, String password, String firstName, String lastName, Boolean verified, Timestamp verifiedOn){
        this.id = UUID.randomUUID();
        this.userName = userName;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.verified = verified;
        this.verifiedOn = verifiedOn;
    }

    public User(){

    }

//    @Column(name = "version_num")
//    @Version
//    private int version;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Timestamp getAccountCreated() {
        return accountCreated;
    }

    public void setAccountCreated(Timestamp account_created) {
        this.accountCreated = account_created;
    }

    public Timestamp getAccountUpdated() {
        return accountUpdated;
    }

    public void setAccountUpdated(Timestamp account_updated) {
        this.accountUpdated = account_updated;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public Timestamp getVerifiedOn() {
        return verifiedOn;
    }

    public void setVerifiedOn(Timestamp verifiedOn) {
        this.verifiedOn = verifiedOn;
    }

//    public int getVersion() {
//        return version;
//    }
//
//    public void setVersion(int version) {
//        this.version = version;
//    }
}
