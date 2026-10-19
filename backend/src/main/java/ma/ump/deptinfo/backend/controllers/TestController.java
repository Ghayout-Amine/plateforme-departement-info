
package ma.ump.deptinfo.backend.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/test/protected")
    public String protectedTest() {
        return "JWT valide ! Accès autorisé.";
    }
}
