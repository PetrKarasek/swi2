package cz.osu.pesa.swi22025.todelete;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {
    @Autowired
    Sender sender;

    @PostMapping(value = "/sender")
    public String send() {
        sender.send("Hey");
        return "Message sent";
    }
}
