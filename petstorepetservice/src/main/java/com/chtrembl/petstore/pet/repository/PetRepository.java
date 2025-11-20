package com.chtrembl.petstore.pet.repository;

import com.chtrembl.petstore.pet.model.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {
    // Simple method - let JPA handle it with the StatusConverter
    @Query("SELECT p FROM Pet p")
    List<Pet> findAllPets();
}

