package com.chtrembl.petstore.pet.service;

import com.chtrembl.petstore.pet.model.Pet;
import com.chtrembl.petstore.pet.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;

    @Transactional(readOnly = true)
    public List<Pet> findPetsByStatus(List<String> status) {
        log.info("Finding pets with status: {}", status);
        try {
            List<Pet> allPets = petRepository.findAll();
            log.info("Loaded {} pets from database", allPets.size());
            
            List<Pet> filtered = allPets.stream()
                    .filter(pet -> {
                        if (pet.getStatus() == null) {
                            log.warn("Pet {} has null status", pet.getId());
                            return false;
                        }
                        return status.contains(pet.getStatus().getValue());
                    })
                    .toList();
            
            log.info("Filtered to {} pets with status: {}", filtered.size(), status);
            return filtered;
        } catch (Exception e) {
            log.error("Error finding pets by status: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Optional<Pet> findPetById(Long petId) {
        log.info("Finding pet with id: {}", petId);
        return petRepository.findById(petId);
    }

    @Transactional(readOnly = true)
    public List<Pet> getAllPets() {
        log.info("Getting all pets");
        return petRepository.findAll();
    }

    public int getPetCount() {
        return (int) petRepository.count();
    }
}