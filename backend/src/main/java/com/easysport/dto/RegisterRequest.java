package com.easysport.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public class RegisterRequest {

    @NotBlank(message = "Vui lòng nhập họ và tên")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Size(max = 25)
    private String phone;

    private String address = "";
    private String dateOfBirth = "";

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    @Size(min = 8, max = 128, message = "Mật khẩu phải có từ 8 đến 128 ký tự.")
    @Pattern(regexp = "(?s).*[0-9].*", message = "Mật khẩu cần có ít nhất một chữ số.")
    private String password;

    public RegisterRequest() {}

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

