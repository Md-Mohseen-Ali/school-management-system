package com.schoolmanagement.modules.attendance.repository;

public interface AttendanceStudentAggregate {

    Long getStudentId();

    String getAdmissionNumber();

    String getFirstName();

    String getLastName();

    Long getTotalRecords();

    Long getPresentCount();

    Long getAbsentCount();

    Long getLateCount();

    Long getExcusedCount();
}