package dev.gustavorosa.cpsystem.api.controller;

import dev.gustavorosa.cpsystem.api.request.CreateClientRequest;
import dev.gustavorosa.cpsystem.api.request.UpdateClientRequest;
import dev.gustavorosa.cpsystem.model.Client;
import dev.gustavorosa.cpsystem.service.ClientService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("v1/client")
@Slf4j
public class ClientController {

    @Autowired
    private ClientService clientService;

    @PostMapping
    public ResponseEntity<Long> createClient(@Valid @RequestBody CreateClientRequest request) {
        log.info("[Entry - ClientController.createClient] - Creating client: {}", request);
        Long newClientId = clientService.createClient(request);
        log.info("[Exit - ClientController.createClient] - Client created successfully. New client id: {}", newClientId);
        return ResponseEntity.created(URI.create("/v1/client/" + newClientId)).build();
    }

    @GetMapping
    public ResponseEntity<List<Client>> findAllClients() {
        log.info("[Entry - ClientController.findAllClients] - Searching for all clients");
        List<Client> clients = clientService.findAllClients();
        log.info("[Exit - ClientController.findAllClients] - Found {} clients", clients.size());
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Client> findClientById(@PathVariable Long id) {
        log.info("[Entry - ClientController.findClientById] - Searching for client with id: {}", id);
        Client client = clientService.findClientById(id);
        log.info("[Exit - ClientController.findClientById] - Client found: {}", client);
        return ResponseEntity.ok(client);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Client> updateClient(@PathVariable Long id, @Valid @RequestBody UpdateClientRequest request) {
        log.info("[Entry - ClientController.updateClient] - Updating client with id: {}, request: {}", id, request);
        Client updatedClient = clientService.updateClient(id, request);
        log.info("[Exit - ClientController.updateClient] - Client updated successfully: {}", updatedClient);
        return ResponseEntity.ok(updatedClient);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        log.info("[Entry - ClientController.deleteClient] - Deleting client with id: {}", id);
        clientService.deleteClient(id);
        log.info("[Exit - ClientController.deleteClient] - Client deleted successfully");
        return ResponseEntity.noContent().build();
    }
}
