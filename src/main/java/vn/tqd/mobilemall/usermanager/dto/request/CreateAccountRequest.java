package vn.tqd.mobilemall.usermanager.dto.request;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequest implements java.io.Serializable{
    private static final long serialVersionUID = 1L;
    private String username;
    private String password;
    private String email;
}