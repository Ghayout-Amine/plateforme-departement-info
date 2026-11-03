package ma.ump.deptinfo.backend.dtos;

import ma.ump.deptinfo.backend.entities.Role;

public class LoginResponse {

    private String token;
    private String tokenType;
    private Role role;
    private String email;
    private String firstName;
    private String lastName;

    public LoginResponse() {
    }

    public LoginResponse(String token, String tokenType) {
        this.token = token;
        this.tokenType = tokenType;
    }

    public LoginResponse(String token, String tokenType, Role role, String email, String firstName, String lastName) {
        this.token = token;
        this.tokenType = tokenType;
        this.role = role;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
}
