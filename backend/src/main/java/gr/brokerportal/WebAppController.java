package gr.brokerportal;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// the React router owns these paths; a refresh on one of them still has to load the app
@Controller
class WebAppController {

    @GetMapping({ "/login", "/submissions", "/submissions/new", "/submissions/{id}", "/submissions/{id}/edit" })
    String app() {
        return "forward:/index.html";
    }
}
