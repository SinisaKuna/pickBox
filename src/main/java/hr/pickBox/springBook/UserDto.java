package hr.pickBox.springBook;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto
{
    private Long id;
    @NotEmpty(message = "ime mora biti upisano")
    private String firstName;
    @NotEmpty(message = "prezime mora biti upisano")
    private String lastName;
    @NotEmpty(message = "oib mora biti upisan")
    private String oib;
    @NotEmpty(message = "email mora biti upisan")
    @Email
    private String email;
    @NotEmpty(message = "lozinka mora biti upisana")
    private String password;
}
