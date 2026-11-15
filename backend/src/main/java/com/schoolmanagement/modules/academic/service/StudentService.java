package com.schoolmanagement.modules.academic.service;

import java.time.LocalDate;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.schoolmanagement.modules.academic.dto.CreateStudentRequest;
import com.schoolmanagement.modules.academic.dto.StudentResponse;
import com.schoolmanagement.modules.academic.dto.UpdateStudentRequest;
import com.schoolmanagement.modules.academic.entity.Student;
import com.schoolmanagement.modules.academic.entity.StudentStatus;
import com.schoolmanagement.modules.academic.repository.StudentRepository;

@Service
@Transactional
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    // Register student
    public StudentResponse createStudent(CreateStudentRequest request) {

        String admissionNumber = request.admissionNumber()
                .trim()
                .toUpperCase(Locale.ROOT);

        if (studentRepository.existsByAdmissionNumberIgnoreCase(
                admissionNumber)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Admission number already exists"
            );
        }

        Student student = new Student();

        student.setAdmissionNumber(admissionNumber);
        student.setFirstName(request.firstName().trim());
        student.setLastName(request.lastName().trim());
        student.setDateOfBirth(request.dateOfBirth());

        student.setAdmissionDate(
                request.admissionDate() != null
                        ? request.admissionDate()
                        : LocalDate.now()
        );

        student.setStatus(StudentStatus.ACTIVE);

        return toResponse(studentRepository.save(student));
    }

    // Get all students with search and pagination
    @Transactional(readOnly = true)
    public Page<StudentResponse> getAllStudents(
            String search,
            StudentStatus status,
            Pageable pageable) {

        Specification<Student> specification = (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Apply status filter only when supplied
            if (status != null) {
                predicates.add(
                        cb.equal(root.get("status"), status)
                );
            }

            // Apply text search only when supplied
            if (search != null && !search.isBlank()) {

                String pattern = "%"
                        + search.trim().toLowerCase(Locale.ROOT)
                        + "%";

                Predicate admissionMatch = cb.like(
                        cb.lower(root.<String>get("admissionNumber")),
                        pattern
                );

                Predicate firstNameMatch = cb.like(
                        cb.lower(root.<String>get("firstName")),
                        pattern
                );

                Predicate lastNameMatch = cb.like(
                        cb.lower(root.<String>get("lastName")),
                        pattern
                );

                predicates.add(
                        cb.or(
                                admissionMatch,
                                firstNameMatch,
                                lastNameMatch
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return studentRepository
                .findAll(specification, pageable)
                .map(this::toResponse);
    }

    // Get student by ID
    @Transactional(readOnly = true)
    public StudentResponse getStudentById(Long id) {

        return toResponse(findStudent(id));
    }

    // Update student
    public StudentResponse updateStudent(
            Long id,
            UpdateStudentRequest request) {

        Student student = findStudent(id);

        student.setFirstName(request.firstName().trim());
        student.setLastName(request.lastName().trim());
        student.setDateOfBirth(request.dateOfBirth());
        student.setStatus(request.status());

        return toResponse(studentRepository.save(student));
    }

    // Soft delete: deactivate instead of physically deleting
    public void deactivateStudent(Long id) {

        Student student = findStudent(id);

        student.setStatus(StudentStatus.INACTIVE);

        studentRepository.save(student);
    }

    private Student findStudent(Long id) {

        return studentRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Student not found with ID: " + id
                        ));
    }

    private StudentResponse toResponse(Student student) {

        return new StudentResponse(
                student.getId(),
                student.getAdmissionNumber(),
                student.getFirstName(),
                student.getLastName(),
                student.getDateOfBirth(),
                student.getAdmissionDate(),
                student.getStatus()
        );
    }
}