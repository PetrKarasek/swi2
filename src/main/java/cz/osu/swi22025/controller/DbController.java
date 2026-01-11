package cz.osu.swi22025.controller;

import cz.osu.swi22025.model.ChatRoom;
import cz.osu.swi22025.model.ChatUser;
import cz.osu.swi22025.service.DbService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@CrossOrigin
public class DbController {
    @Autowired
    private DbService dbService;

    @GetMapping(value = "/users")
    public ResponseEntity<List<String>> getUsers() {
        return dbService.getUsers();
    }

    @GetMapping(value = "/chatrooms")
    public ResponseEntity<List<ChatRoom>> getChatRooms(@RequestParam String username) {
        return dbService.getChatRooms(username);
    }
}
