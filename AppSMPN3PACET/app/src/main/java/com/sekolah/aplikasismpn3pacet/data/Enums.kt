package com.sekolah.aplikasismpn3pacet.data

enum class UserRole {
    STUDENT, TEACHER, ADMIN
}

enum class Gender {
    MALE, FEMALE
}

enum class BookLoanStatus {
    BORROWED, RETURNED, OVERDUE
}

enum class AttendanceStatus {
    PRESENT, ABSENT, LATE, SICK, PERMIT
}

enum class CheckInMethod {
    QR_CODE, MANUAL, NFC
}

enum class RuleCategory {
    VIOLATION, // General/Other Violation
    VIOLATION_LATE, // Terlambat
    VIOLATION_BEHAVIOR, // Perilaku
    VIOLATION_ATTRIBUTE, // Atribut/Seragam
    ACHIEVEMENT
}

enum class RuleSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class RecordStatus {
    PENDING, APPROVED, REJECTED
}

enum class PetStatus {
    HEALTHY, SICK, HAPPY, SAD, DEAD
}

enum class ActivityType {
    STUDY, ATTENDANCE, DISCIPLINE, LIBRARY
}

enum class IncidentType {
    PHYSICAL, VERBAL, CYBER, SOCIAL
}

enum class ReportStatus {
    PENDING, INVESTIGATING, RESOLVED, CLOSED
}

enum class ReportPriority {
    LOW, MEDIUM, HIGH, URGENT
}

enum class NotificationType {
    INFO, WARNING, ALERT, SUCCESS
}

enum class NotificationCategory {
    ATTENDANCE, DISCIPLINE, LIBRARY, PET, BULLYING, GENERAL
}

enum class SubmissionStatus {
    PENDING, GRADED, REJECTED
}
