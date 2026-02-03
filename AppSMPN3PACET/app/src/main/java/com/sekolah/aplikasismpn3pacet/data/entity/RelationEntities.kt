package com.sekolah.aplikasismpn3pacet.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class StudentWithUser(
    @Embedded val student: Student,
    @Relation(
        parentColumn = "user_id",
        entityColumn = "id"
    )
    val user: User,
    @Relation(
        parentColumn = "class_id",
        entityColumn = "id"
    )
    val classEntity: ClassEntity?
)

data class DisciplineRecordWithRule(
    @Embedded val record: DisciplineRecord,
    @Relation(
        parentColumn = "rule_id",
        entityColumn = "id"
    )
    val rule: DisciplineRule
)

data class LiteracyLogWithDetails(
    @Embedded val log: LiteracyLog,
    @Relation(
        entity = Student::class,
        parentColumn = "student_id",
        entityColumn = "id"
    )
    val student: StudentWithUser
)

data class BullyingReportWithReporter(
    @Embedded val report: BullyingReport,
    @Relation(
        entity = Student::class,
        parentColumn = "reporter_id",
        entityColumn = "id"
    )
    val reporter: StudentWithUser?
)
