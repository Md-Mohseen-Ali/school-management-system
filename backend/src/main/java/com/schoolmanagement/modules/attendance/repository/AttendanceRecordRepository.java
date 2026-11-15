package com.schoolmanagement.modules.attendance.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.schoolmanagement.modules.attendance.entity.AttendanceRecord;

public interface AttendanceRecordRepository
        extends JpaRepository<AttendanceRecord, Long> {

    List<AttendanceRecord>
    findByAttendanceSession_IdOrderByStudent_AdmissionNumberAsc(
            Long sessionId
    );

    Optional<AttendanceRecord>
    findByAttendanceSession_IdAndStudent_Id(
            Long sessionId,
            Long studentId
    );

    long countByAttendanceSession_Id(Long sessionId);

    // Student summary across all sections
    @Query("""
        SELECT
            st.id AS studentId,
            st.admissionNumber AS admissionNumber,
            st.firstName AS firstName,
            st.lastName AS lastName,
            COUNT(r.id) AS totalRecords,

            SUM(CASE WHEN r.attendanceStatus =
                com.schoolmanagement.modules.attendance.entity.AttendanceRecordStatus.PRESENT
                THEN 1 ELSE 0 END) AS presentCount,

            SUM(CASE WHEN r.attendanceStatus =
                com.schoolmanagement.modules.attendance.entity.AttendanceRecordStatus.ABSENT
                THEN 1 ELSE 0 END) AS absentCount,

            SUM(CASE WHEN r.attendanceStatus =
                com.schoolmanagement.modules.attendance.entity.AttendanceRecordStatus.LATE
                THEN 1 ELSE 0 END) AS lateCount,

            SUM(CASE WHEN r.attendanceStatus =
                com.schoolmanagement.modules.attendance.entity.AttendanceRecordStatus.EXCUSED
                THEN 1 ELSE 0 END) AS excusedCount

        FROM AttendanceRecord r
        JOIN r.student st
        JOIN r.attendanceSession sess

        WHERE st.id = :studentId
          AND sess.attendanceDate BETWEEN :fromDate AND :toDate
          AND sess.status IN (
              com.schoolmanagement.modules.attendance.entity.AttendanceSessionStatus.SUBMITTED,
              com.schoolmanagement.modules.attendance.entity.AttendanceSessionStatus.LOCKED
          )

        GROUP BY st.id, st.admissionNumber, st.firstName, st.lastName
        """)
    Optional<AttendanceStudentAggregate> summarizeStudent(
            @Param("studentId") Long studentId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    // Student summary restricted to one section
    @Query("""
        SELECT
            st.id AS studentId,
            st.admissionNumber AS admissionNumber,
            st.firstName AS firstName,
            st.lastName AS lastName,
            COUNT(r.id) AS totalRecords,

            SUM(CASE WHEN r.attendanceStatus =
                com.schoolmanagement.modules.attendance.entity.AttendanceRecordStatus.PRESENT
                THEN 1 ELSE 0 END) AS presentCount,

            SUM(CASE WHEN r.attendanceStatus =
                com.schoolmanagement.modules.attendance.entity.AttendanceRecordStatus.ABSENT
                THEN 1 ELSE 0 END) AS absentCount,

            SUM(CASE WHEN r.attendanceStatus =
                com.schoolmanagement.modules.attendance.entity.AttendanceRecordStatus.LATE
                THEN 1 ELSE 0 END) AS lateCount,

            SUM(CASE WHEN r.attendanceStatus =
                com.schoolmanagement.modules.attendance.entity.AttendanceRecordStatus.EXCUSED
                THEN 1 ELSE 0 END) AS excusedCount

        FROM AttendanceRecord r
        JOIN r.student st
        JOIN r.attendanceSession sess

        WHERE st.id = :studentId
          AND sess.section.id = :sectionId
          AND sess.attendanceDate BETWEEN :fromDate AND :toDate
          AND sess.status IN (
              com.schoolmanagement.modules.attendance.entity.AttendanceSessionStatus.SUBMITTED,
              com.schoolmanagement.modules.attendance.entity.AttendanceSessionStatus.LOCKED
          )

        GROUP BY st.id, st.admissionNumber, st.firstName, st.lastName
        """)
    Optional<AttendanceStudentAggregate> summarizeStudentInSection(
            @Param("studentId") Long studentId,
            @Param("sectionId") Long sectionId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    // Section report, grouped by student
    @Query("""
        SELECT
            st.id AS studentId,
            st.admissionNumber AS admissionNumber,
            st.firstName AS firstName,
            st.lastName AS lastName,
            COUNT(r.id) AS totalRecords,

            SUM(CASE WHEN r.attendanceStatus =
                com.schoolmanagement.modules.attendance.entity.AttendanceRecordStatus.PRESENT
                THEN 1 ELSE 0 END) AS presentCount,

            SUM(CASE WHEN r.attendanceStatus =
                com.schoolmanagement.modules.attendance.entity.AttendanceRecordStatus.ABSENT
                THEN 1 ELSE 0 END) AS absentCount,

            SUM(CASE WHEN r.attendanceStatus =
                com.schoolmanagement.modules.attendance.entity.AttendanceRecordStatus.LATE
                THEN 1 ELSE 0 END) AS lateCount,

            SUM(CASE WHEN r.attendanceStatus =
                com.schoolmanagement.modules.attendance.entity.AttendanceRecordStatus.EXCUSED
                THEN 1 ELSE 0 END) AS excusedCount

        FROM AttendanceRecord r
        JOIN r.student st
        JOIN r.attendanceSession sess

        WHERE sess.section.id = :sectionId
          AND sess.attendanceDate BETWEEN :fromDate AND :toDate
          AND sess.status IN (
              com.schoolmanagement.modules.attendance.entity.AttendanceSessionStatus.SUBMITTED,
              com.schoolmanagement.modules.attendance.entity.AttendanceSessionStatus.LOCKED
          )

        GROUP BY st.id, st.admissionNumber, st.firstName, st.lastName
        ORDER BY st.admissionNumber
        """)
    List<AttendanceStudentAggregate> summarizeSection(
            @Param("sectionId") Long sectionId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}