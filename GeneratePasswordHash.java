import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GeneratePasswordHash {
    public static void main(String[] args) {
        String password = "Recruiter@123";
        System.out.println(new BCryptPasswordEncoder().encode(password));
    }
}
