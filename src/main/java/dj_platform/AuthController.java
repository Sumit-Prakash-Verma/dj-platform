
package dj_platform;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            SecurityContextRepository securityContextRepository) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityContextRepository = securityContextRepository;
    }

    // 1. USER REGISTRATION API
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @RequestBody AppUser user) {

        if (user.getUsername() == null
                || user.getUsername().isBlank()
                || user.getEmail() == null
                || user.getEmail().isBlank()
                || user.getPassword() == null
                || user.getPassword().length() < 8) {

            return ResponseEntity.badRequest().body(
                    Map.of("message",
                            "Username and email are required; password must be at least 8 characters."));
        }

        if (user.getUsername().length() > 100
                || user.getEmail().length() > 150) {

            return ResponseEntity.badRequest().body(
                    Map.of("message",
                            "Username or email is too long."));
        }

        if (userRepository.existsByUsername(user.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("message", "Username already exists."));
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("message", "Email already registered."));
        }

        // Password ko BCrypt hash mein convert karna
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of("message", "Registration successful!"));
    }

    // 2. USER LOGIN API
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String username = request.get("username");
        String password = request.get("password");

        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {

            return ResponseEntity.badRequest().body(
                    Map.of("message",
                            "Username and password are required."));
        }

        AppUser user = userRepository.findByUsername(username)
                .orElse(null);

        if (user == null
                || !passwordEncoder.matches(
                        password, user.getPassword())) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("message",
                            "Invalid username or password."));
        }

        // Purana session invalidate karna
        HttpSession oldSession = httpRequest.getSession(false);

        if (oldSession != null) {
            oldSession.invalidate();
        }

        // Naya session create karna
        HttpSession session = httpRequest.getSession(true);

        session.setAttribute("loggedInUserId", user.getId());
        session.setAttribute("loggedInUsername", user.getUsername());

        // Spring Security authentication create karna
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                user.getUsername(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        // SecurityContext create aur set karna
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Authentication ko HTTP session mein save karna
        securityContextRepository.saveContext(
                securityContext,
                httpRequest,
                httpResponse);

        return ResponseEntity.ok(
                Map.of(
                        "message", "Login successful!",
                        "username", user.getUsername()));
    }

    // 3. CURRENT LOGIN SESSION CHECK API
    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> getCurrentUser(
            HttpSession session) {

        String username = (String) session.getAttribute("loggedInUsername");

        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("message", "User is not logged in."));
        }

        return ResponseEntity.ok(
                Map.of(
                        "username", username,
                        "message", "User is logged in."));
    }

    // 4. USER LOGOUT API
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(
                Map.of("message", "Logout successful!"));
    }
}