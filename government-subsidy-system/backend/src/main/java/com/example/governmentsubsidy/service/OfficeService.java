package com.example.governmentsubsidy.service;

import com.example.governmentsubsidy.entity.Office;
import com.example.governmentsubsidy.repository.OfficeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OfficeService {

    private final OfficeRepository officeRepository;

    public OfficeService(OfficeRepository officeRepository) {
        this.officeRepository = officeRepository;
    }

    public List<Office> getAllOffices() {
        return officeRepository.findAll();
    }

    public Optional<Office> getOfficeById(Long id) {
        return officeRepository.findById(id);
    }

    public Office createOffice(Office office) {
        return officeRepository.save(office);
    }

    public Office updateOffice(Long id, Office updatedOffice) {
        return officeRepository.findById(id).map(existing -> {
            existing.setName(updatedOffice.getName());
            existing.setAddress(updatedOffice.getAddress());
            existing.setActive(updatedOffice.isActive());
            return officeRepository.save(existing);
        }).orElseThrow(() -> new IllegalArgumentException("Office not found with id " + id));
    }

    public void deleteOffice(Long id) {
        officeRepository.deleteById(id);
    }
}
